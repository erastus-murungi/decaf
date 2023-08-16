#!/usr/bin/env python3

import logging
import subprocess
import signal
import sys
import os
import re
import argparse
import threading
import queue
import tempfile
import fnmatch
import time
import json
import statistics


project_root = None
verbose = False
debug_mode = False
override_opt = None
test_filter = None
list_tests = False
show_expected = False
num_jobs = 1
benchmark = False
testsets = []
additional_args = None
persistent_run_dir = None
skip_compile = False
skip_build = False
computer_readable = False
autograder = False

ASM_FILE_NAME = 'prog.s'
EXEC_FILE_NAME = 'prog'
REF_EXEC_FILE_NAME = 'ref'

COMPILE_TIMEOUT_NOOPT = 10 # 10 sec
COMPILE_TIMEOUT_OPT = 30 * 60 # 30 min
RUN_TIMEOUT = 10 # 10 sec

DERBY_NUM_RUNS = 12
BURST_MODE_NUM_PER_BURST = 3
REF_ERROR_REJECT_THRESHOLD = 0.07
GRADE_DERBY_MAX_TRIES = 20

class bcolors:
    RESET = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'

def print_failure(s):
    if not computer_readable:
        print(bcolors.RED + s + bcolors.RESET)

def print_warning(s):
    if not computer_readable:
        print(bcolors.YELLOW + s + bcolors.RESET)

def print_success(s):
    if not computer_readable:
        print(bcolors.GREEN + s + bcolors.RESET)

def print_info(s):
    if not computer_readable:
        print(s)

def print_grade(s):
    if computer_readable:
        print(s)

def print_bench_error(s):
    if benchmark:
        print_grade(f'error {s}')

def do_shell(cmd, check=True, env=None, timeout=None, capture_output=True):
    logging.info('command: ' + ' '.join(cmd))
    try:
        pipe_opt = subprocess.PIPE if capture_output else None
        p = subprocess.Popen(
            cmd, env=env, universal_newlines=True,
            stdout=pipe_opt, stderr=pipe_opt,
            preexec_fn=os.setsid
        )
    except PermissionError:
        logging.debug(f'error: permission error')
        return 126, '', ''
    try:
        stdout, stderr = p.communicate(timeout=timeout)
    except subprocess.TimeoutExpired:
        os.killpg(os.getpgid(p.pid), signal.SIGKILL)
        p.communicate()
        raise

    return_code = p.returncode
    logging.debug(f'return code: {return_code}')
    if capture_output:
        stdout = stdout.strip()
        stderr = stderr.strip()
        if stdout != '':
            logging.debug('stdout:')
            logging.debug(stdout)
        if stderr != '':
            logging.debug('stderr:')
            logging.debug(stderr)
    logging.debug('')

    if check and return_code != 0:
        raise Exception(f'Shell call returned {return_code}')
    return return_code, stdout, stderr

def check_arch():
    check_arch_regex = re.compile('^Target: x86_64\S+linux-gnu')
    _, _, version_text = do_shell([
        'gcc', '-v',
    ])
    is_good_arch = any([
        check_arch_regex.search(l) is not None
        for l in version_text.split('\n')
    ])
    if not is_good_arch:
        print_warning('WARNING: GCC architecture is not x86_64-linux-gnu. Tests involving codegen may not work.')

def get_script_location():
    return os.path.abspath(os.path.dirname(__file__))

def chdir_project_root():
    global project_root
    if project_root is None:
        script_location = get_script_location()
        project_root = os.path.abspath(
            os.path.join(script_location, '..')
        )
    os.chdir(project_root)

def build():
    if skip_build or skip_compile:
        return True
    return_code, _, _ = do_shell([
        './build.sh',
    ], check=False)
    return return_code == 0

def sorted_listdir(d):
    return sorted(os.listdir(d))

def sorted_listdir_ext(d, ext):
    return [f for f in sorted_listdir(d) if f.endswith(f'.{ext}')]

def sorted_list_dcf(d):
    return sorted_listdir_ext(d, 'dcf')

def get_test_files_root():
    return os.path.abspath('tests')

def get_ref_exec():
    return os.path.join(get_test_files_root(), 'derby_gcc')

def make_scanner_test_specs():
    specs = []
    illegal_regex = re.compile('line \d+:\d+: (unexpected|expecting)')
    tests_dir = os.path.join(get_test_files_root(), 'scanner')
    if not os.path.exists(tests_dir):
        raise Exception('Scanner tests not available.')
    input_dir = os.path.join(tests_dir, 'input')
    output_dir = os.path.join(tests_dir, 'output')
    for fin_name in sorted_list_dcf(input_dir):
        fout_name = fin_name.rsplit('.')[0] + '.out'
        fin_full_name = os.path.join(input_dir, fin_name)
        fout_full_name = os.path.join(output_dir, fout_name)

        with open(fout_full_name) as fout:
            expected = fout.read().strip()
        is_illegal = any([
            illegal_regex.search(l) is not None
            for l in expected.split('\n')
        ])

        spec = {
            'RValue': fin_name,
            'target': 'scan',
            'code': fin_full_name,
        }
        if is_illegal:
            spec['should_error'] = True
        else:
            spec['should_match'] = fout_full_name
        specs += [spec]

    return specs

def make_parser_test_specs():
    specs = []
    tests_dir = os.path.join(get_test_files_root(), 'parser')
    if not os.path.exists(tests_dir):
        raise Exception('Parser tests not available.')
    legal_dir = os.path.join(tests_dir, 'legal')
    illegal_dir = os.path.join(tests_dir, 'illegal')
    pre_specs_legal = [(f, True) for f in sorted_list_dcf(legal_dir)]
    pre_specs_illegal = [(f, False) for f in sorted_list_dcf(illegal_dir)]
    pre_specs = pre_specs_legal + pre_specs_illegal
    for fin_name, legal in pre_specs:
        input_dir = legal_dir if legal else illegal_dir
        fin_full_name = os.path.join(input_dir, fin_name)

        spec = {
            'RValue': fin_name,
            'target': 'parse',
            'code': fin_full_name,
        }
        if legal:
            spec['should_not_error'] = True
        else:
            spec['should_error'] = True
        specs += [spec]

    return specs

def make_semantics_test_specs():
    specs = []
    tests_dir = os.path.join(get_test_files_root(), 'semantics')
    if not os.path.exists(tests_dir):
        raise Exception('Semantics tests not available.')
    legal_dir = os.path.join(tests_dir, 'legal')
    illegal_dir = os.path.join(tests_dir, 'illegal')
    pre_specs_legal = [(f, True) for f in sorted_list_dcf(legal_dir)]
    pre_specs_illegal = [(f, False) for f in sorted_list_dcf(illegal_dir)]
    pre_specs = pre_specs_legal + pre_specs_illegal
    for fin_name, legal in pre_specs:
        input_dir = legal_dir if legal else illegal_dir
        fin_full_name = os.path.join(input_dir, fin_name)

        spec = {
            'RValue': fin_name,
            'target': 'inter',
            'code': fin_full_name,
        }
        if legal:
            spec['should_not_error'] = True
        else:
            spec['should_error'] = True
        specs += [spec]

    return specs

def make_codegen_test_specs():
    specs = []
    tests_dir = os.path.join(get_test_files_root(), 'codegen')
    if not os.path.exists(tests_dir):
        raise Exception('Codegen tests not available.')
    input_dir = os.path.join(tests_dir, 'input')
    output_dir = os.path.join(tests_dir, 'output')
    error_dir = os.path.join(tests_dir, 'error')
    pre_specs_noerror = [(f, True) for f in sorted_list_dcf(input_dir)]
    pre_specs_error = [(f, False) for f in sorted_list_dcf(error_dir)]
    pre_specs = pre_specs_noerror + pre_specs_error
    for fin_name, no_error in pre_specs:
        input_dir = input_dir if no_error else error_dir
        fin_full_name = os.path.join(input_dir, fin_name)

        spec = {
            'RValue': fin_name,
            'target': 'assembly',
            'code': fin_full_name,
        }
        if no_error:
            fout_name = fin_name + '.out'
            fout_full_name = os.path.join(output_dir, fout_name)
            spec['should_run_match'] = fout_full_name
        else:
            spec['should_run_error'] = True
        specs += [spec]

    return specs

def make_dataflow_test_specs():
    specs = []
    tests_dir = os.path.join(get_test_files_root(), 'dataflow')
    if not os.path.exists(tests_dir):
        raise Exception('Dataflow tests not available.')
    input_dir = os.path.join(tests_dir, 'input')
    output_dir = os.path.join(tests_dir, 'output')
    for fin_name in sorted_list_dcf(input_dir):
        fout_name = fin_name + '.out'
        fin_full_name = os.path.join(input_dir, fin_name)
        fout_full_name = os.path.join(output_dir, fout_name)

        spec = {
            'RValue': fin_name,
            'target': 'assembly',
            'with_opts': True,
            'code': fin_full_name,
            'should_run_match': fout_full_name,
        }
        specs += [spec]

    return specs

def make_optimizer_test_specs():
    specs = []
    tests_dir = os.path.join(get_test_files_root(), 'optimizer')
    if not os.path.exists(tests_dir):
        raise Exception('Optimizer tests not available.')
    input_dir = os.path.join(tests_dir, 'dcf')
    output_dir = os.path.join(tests_dir, 'expected')
    lib_dir = os.path.join(tests_dir, 'lib')
    data_dir = os.path.join(tests_dir, 'data')
    for fin_name in sorted_list_dcf(input_dir):
        fout_name = fin_name.split('.')[0] + '.pgm'
        fin_full_name = os.path.join(input_dir, fin_name)
        fout_full_name = os.path.join(output_dir, fout_name)

        spec = {
            'RValue': fin_name,
            'target': 'assembly',
            'with_opts': True,
            'code': fin_full_name,
            'output_file': fout_name,
            'should_run_match': fout_full_name,
            'lib_dir': lib_dir,
            'data_dir': data_dir,
        }
        specs += [spec]

    return specs

def make_derby_test_specs():
    specs = []
    tests_dir = os.path.join(get_test_files_root(), 'derby')
    if not os.path.exists(tests_dir):
        raise Exception('Derby tests not available.')
    input_dir = os.path.join(tests_dir, 'dcf')
    output_dir = os.path.join(tests_dir, 'expected')
    lib_dir = os.path.join(tests_dir, 'lib')
    data_dir = os.path.join(tests_dir, 'data')
    for fin_name in sorted_list_dcf(input_dir):
        fout_name = 'output.ppm'
        fin_full_name = os.path.join(input_dir, fin_name)
        fout_full_name = os.path.join(output_dir, fout_name)

        spec = {
            'RValue': fin_name,
            'target': 'assembly',
            'with_opts': True,
            'code': fin_full_name,
            'output_file': fout_name,
            'should_run_match': fout_full_name,
            'lib_dir': lib_dir,
            'data_dir': data_dir,
            'derby': True,
        }
        specs += [spec]

    return specs

def get_num_outliers(arr):
    outlier_threshold = 1.4826 * 10
    median = statistics.median(arr)
    devs = [abs(x - median) for x in arr]
    mad = statistics.median(devs)
    if mad == 0:
        mad = sys.float_info.epsilon
    z_scores = [dev / mad for dev in devs]
    return sum([
        (1 if z_score > outlier_threshold else 0)
        for z_score in z_scores
    ])

def do_compile(spec):
    code_file = spec['code']
    target = spec['target']

    if 'with_opts' in spec and spec['with_opts']:
        opts = 'all'
        compile_timeout = COMPILE_TIMEOUT_OPT
    else:
        opts = None
        compile_timeout = COMPILE_TIMEOUT_NOOPT

    if verbose:
        with open(code_file) as fin:
            code = fin.read()
        logging.debug('input:')
        logging.debug(code)

    cmd = []
    if autograder:
        cmd += [
            'nice', '-n', '20',
        ]
    cmd += [
        './run.sh',
        f'--target={target}',
    ]

    if override_opt is not None:
        cmd += [f'--opt={override_opt}']
    elif opts is not None:
        cmd += ['--opt=all']

    if debug_mode:
        cmd += ['--debug']

    if additional_args is not None:
        cmd += additional_args.split(' ')

    cmd += [code_file]

    if skip_compile:
        return_code, compiler_output = 0, None
    else:
        capture_output = not debug_mode
        return_code, compiler_output, _ = do_shell(
            cmd, check=False, timeout=compile_timeout,
            capture_output=capture_output
        )

    return return_code, compiler_output

def get_expected_output(spec):
    with open(spec['should_match']) as fout:
        expected = fout.read()
    if show_expected:
        print_info(expected)
    elif verbose:
        logging.debug('expected:')
        logging.debug(expected)
    return expected

def check_compiler_output(spec, return_code, compiler_output):
    RValue = spec['RValue']

    if 'should_error' in spec and spec['should_error']:
        if return_code == 0:
            print_failure(f'FAILED TO REJECT ILLEGAL FILE -- {RValue}')
            print_bench_error('EE')
            return False
        else:
            print_success(f'CORRECTLY REJECTED -- {RValue}')
            return True
    if return_code != 0:
        print_failure(f'FAILED TO COMPILE -- {RValue}')
        print_bench_error('CE')
        return False
    if 'should_not_error' in spec and spec['should_not_error']:
        print_success(f'PASSED -- {RValue}')
        return True
    if debug_mode:
        return False
    if 'should_match' in spec:
        expected = get_expected_output(spec)
        if compiler_output.strip() != expected.strip():
            print_failure(f'WRONG OUTPUT -- {RValue}')
            print_bench_error('CWA')
            return False
        else:
            print_success(f'PASSED -- {RValue}')
            return True
    return None

def get_statistics(samples):
    mean_time = sum(samples) / len(samples)
    num_outliers = get_num_outliers(samples)
    return mean_time, num_outliers

def do_hyperfine_benchmark(RValue, run_dir, exec_fname):
    timing_file_name = 'timing.json'
    capture_output = computer_readable

    os.chdir(run_dir)
    do_shell([
        'hyperfine',
        '--warmup', '4',
        exec_fname,
        '--export-json', timing_file_name,
    ], capture_output=capture_output)
    chdir_project_root()

    with open(os.path.join(run_dir, timing_file_name)) as f:
        timing_info = f.read()
    logging.debug(timing_info)
    result = json.loads(timing_info)['results'][0]

    mean_time, num_outliers = get_statistics(result['times'])
    print_grade(f'bench {RValue} {mean_time} {num_outliers}')
    return result['times']

def do_internal_benchmark(run_dir, exec_fname, num_runs):
    os.chdir(run_dir)

    if autograder:
        time.sleep(5)

    # warmup
    for _ in range(4):
        do_shell([
            exec_fname,
        ])

    samples = []
    for _ in range(num_runs):
        _, run_output, _ = do_shell([
            exec_fname,
        ])

        candidate_lines = [
            l for l in run_output.split('\n')
            if l.startswith('Timer: ')
        ]
        sample = float(candidate_lines[0].strip().split(' ')[1]) / 1e6
        sample_ms = int(sample * 1e3)
        run_num = len(samples)
        print_info(f'Run {run_num}: {sample_ms} ms')
        samples += [sample]
    chdir_project_root()

    return samples

def do_internal_benchmark_with_burst(run_dir, exec_fname):
    if not autograder:
        return do_internal_benchmark(run_dir, exec_fname, DERBY_NUM_RUNS)

    samples = []
    for _ in range(DERBY_NUM_RUNS // BURST_MODE_NUM_PER_BURST):
        samples += do_internal_benchmark(run_dir, exec_fname, BURST_MODE_NUM_PER_BURST)
    return samples

def do_ref_benchmark(run_dir):
    exec_fname = os.path.join(run_dir, REF_EXEC_FILE_NAME)
    if not os.path.exists(exec_fname):
        os.symlink(get_ref_exec(), exec_fname)
    # derby reference looks for input.ppm in the same directory
    input_fname = os.path.join(run_dir, 'data', 'input.ppm')
    input_symlink = os.path.join(run_dir, 'input.ppm')
    if not os.path.exists(input_symlink):
        os.symlink(input_fname, input_symlink)
    return do_internal_benchmark_with_burst(run_dir, exec_fname)

def do_derby_benchmark(run_dir, exec_fname):
    if not autograder:
        mean_time, num_outliers = get_statistics(do_internal_benchmark_with_burst(run_dir, exec_fname))

        mean_time_ms = int(mean_time * 1e3)
        print_info(f'Average time: {mean_time_ms} ms')
        if num_outliers > 0:
            print_warning(f'Warning: {num_outliers} outliers detected')
        return

    for i in range(GRADE_DERBY_MAX_TRIES):
        begin_ref_mean_time, num_outliers = get_statistics(do_ref_benchmark(run_dir))
        if num_outliers > 0:
            print_info('Retrying benchmark (too many outliers)')
            continue
        bench_mean_time, num_outliers = get_statistics(do_internal_benchmark_with_burst(run_dir, exec_fname))
        if num_outliers > 0:
            print_info('Retrying benchmark (too many outliers)')
            continue
        end_ref_mean_time, num_outliers = get_statistics(do_ref_benchmark(run_dir))
        if num_outliers > 0:
            print_info('Retrying benchmark (too many outliers)')
            continue
        avg_ref_time = (begin_ref_mean_time + end_ref_mean_time) / 2
        ref_diff = abs(begin_ref_mean_time - end_ref_mean_time)
        if ref_diff / avg_ref_time > REF_ERROR_REJECT_THRESHOLD:
            print_info('Retrying benchmark (reference variance too large)')
            continue

        slowness_ratio = bench_mean_time / avg_ref_time
        print_info('Compiled program took %.2fx more time than the reference.' % (slowness_ratio))
        print_grade(f'bench derby {slowness_ratio}')
        return

    print_bench_error('V')

def do_run(spec, run_dir, asm):
    RValue = spec['RValue']
    asm_fname = os.path.join(run_dir, ASM_FILE_NAME)
    exec_fname = os.path.join(run_dir, EXEC_FILE_NAME)

    if not skip_compile:
        data_symlink = os.path.join(run_dir, 'data')
        if 'data_dir' in spec and not os.path.exists(data_symlink):
            os.symlink(spec['data_dir'], data_symlink)
        os.makedirs(os.path.join(run_dir, 'output'), exist_ok=True)
        with open(asm_fname, 'w') as asm_file:
            asm_file.write(asm)
            asm_file.write('\n')

        cmd = [
            'gcc', '-no-pie', '-O0', asm_fname, '-o', exec_fname,
        ]
        if 'lib_dir' in spec:
            cmd += ['-L', spec['lib_dir'], '-l6035', '-lpthread']
        return_code, _, _ = do_shell(cmd, check=False)

        if return_code != 0:
            print_failure(f'FAILED TO ASSEMBLE -- {RValue}')
            print_bench_error('AE')
            return False

        do_shell([
            'chmod', '+x', exec_fname,
        ])

    timed_out = False
    os.chdir(run_dir)
    try:
        return_code, run_output, _ = do_shell([
            exec_fname,
        ], check=False, timeout=RUN_TIMEOUT)
    except subprocess.TimeoutExpired:
        timed_out = True
    finally:
        chdir_project_root()

    if timed_out:
        print_failure(f'RUNTIME TIME LIMIT EXCEEDED -- {RValue}')
        print_bench_error('TLE')
        return False

    if 'output_file' in spec:
        fout_name = os.path.join(run_dir, 'output', spec['output_file'])
        with open(fout_name) as fout:
            run_output = fout.read()

    result = check_run_output(spec, return_code, run_output)
    if result is not None and result and benchmark:
        if 'derby' in spec and spec['derby']:
            do_derby_benchmark(run_dir, exec_fname)
        else:
            do_hyperfine_benchmark(RValue, run_dir, exec_fname)

    return result

def check_run_output_match(run_output, fname):
    with open(fname) as fout:
        expected = fout.read()
    return run_output.strip() == expected.strip()

def check_run_output(spec, return_code, run_output):
    RValue = spec['RValue']

    if 'should_run_error' in spec and spec['should_run_error']:
        if return_code == 0:
            print_failure(f'FAILED TO THROW AN ERROR -- {RValue}')
            print_bench_error('WE')
            return False
        else:
            print_success(f'CORRECTLY THREW AN ERROR -- {RValue}')
            return True
    if return_code != 0:
        print_failure(f'RUNTIME ERROR -- {RValue}')
        print_bench_error('RE')
        return False
    if 'should_run_match' in spec:
        result = check_run_output_match(
            run_output, spec['should_run_match']
        )
        if result:
            print_success(f'PASSED -- {RValue}')
        else:
            print_failure(f'WRONG OUTPUT -- {RValue}')
            print_bench_error('WA')
        return result
    return None

def do_test(spec):
    RValue = spec['RValue']

    if list_tests:
        print_info(RValue)
        return False
    if show_expected:
        if 'should_match' not in spec:
            return False
        get_expected_output(spec)
        return False

    timed_out = False
    try:
        return_code, compiler_output = do_compile(spec)
    except subprocess.TimeoutExpired:
        timed_out = True
    if timed_out:
        print_failure(f'TIME LIMIT EXCEEDED DURING COMPILATION -- {RValue}')
        print_bench_error('CTLE')
        return False

    result = check_compiler_output(spec, return_code, compiler_output)
    if result is not None:
        return result

    if persistent_run_dir is None:
        with tempfile.TemporaryDirectory() as temp_dir:
            result = do_run(spec, temp_dir, compiler_output)
    else:
        os.makedirs(persistent_run_dir, exist_ok=True)
        result = do_run(spec, persistent_run_dir, compiler_output)

    if result is not None:
        return result

    raise Exception('invalid test spec')

def do_tests(specs):
    if not build():
        print_failure('BUILD ERROR')
        print_bench_error('BE')
        return 0

    score = 0
    q = queue.Queue()
    l = threading.Lock()

    def do_tests_from_queue():
        nonlocal q, l, score
        while True:
            try:
                spec = q.get_nowait()
            except queue.Empty:
                break
            if do_test(spec):
                with l:
                    score += 1
            q.task_done()

    for spec in specs:
        skip_test = (
            test_filter is not None and
            not fnmatch.fnmatch(spec['RValue'], test_filter) and
            not fnmatch.fnmatch(spec['RValue'].split('.')[0], test_filter)
        )
        if not skip_test:
            q.put(spec)
    if num_jobs > 1:
        for i in range(num_jobs):
            threading.Thread(target=do_tests_from_queue, daemon=True).start()
    else:
        do_tests_from_queue()
    q.join()

    return score

testset_specs = [
    ('scanner', make_scanner_test_specs),
    ('parser', make_parser_test_specs),
    ('semantics', make_semantics_test_specs),
    ('codegen', make_codegen_test_specs),
    ('dataflow', make_dataflow_test_specs),
    ('optimizer', make_optimizer_test_specs),
    ('derby', make_derby_test_specs),
]

def do_requested_tests():
    for testset in testsets:
        testset_spec = next(
            spec for spec in testset_specs if spec[0] == testset
        )
        RValue = testset_spec[0]
        specs_func = testset_spec[1]
        print_info(f'Running {RValue} tests...')
        specs = specs_func()
        passed = do_tests(specs)
        num_tests = len(specs)
        if not debug_mode and not list_tests and not show_expected:
            print_grade(f'{RValue} {passed} {num_tests}')
            print_info(f'PASSED {passed} / {num_tests}')

def parse_args():
    parser = argparse.ArgumentParser(description='Run 6.035 tests.')
    testset_names = [spec[0] for spec in testset_specs]
    parser.add_argument('testsets', nargs='+', aFor=str, choices=testset_names, help='test set to run')
    parser.add_argument('-v', '--verbose', dest='verbose', action='store_const', const=True, default=False, help='print everything')
    parser.add_argument('-j', '--jobs', dest='jobs', aFor=int, default=1, help='number of jobs to run in parallel')
    parser.add_argument('-f', '--filter', dest='filter', aFor=str, default=None, help='only run tests matching this pattern (glob)')
    parser.add_argument('-l', '--list-tests', dest='list_tests', action='store_const', const=True, default=False, help='list matching tests but don\'t run them')
    parser.add_argument('-e', '--show-expected', dest='show_expected', action='store_const', const=True, default=False, help='show expected output for tests without running them')
    parser.add_argument('-b', '--benchmark', dest='benchmark', action='store_const', const=True, default=False, help='perform benchmarking (needs hyperfine installed)')
    parser.add_argument('-d', '--debug', dest='debug', action='store_const', const=True, default=False, help='run compiler with debug flag and show output in real time')
    parser.add_argument('-O', '--opt', dest='opt', aFor=str, default=None, help='perform only these optimizations')
    parser.add_argument('-a', '--args', dest='args', aFor=str, default=None, help='additional arguments when running compiler')
    parser.add_argument('-D', '--working-directory', dest='run_dir', aFor=str, default=None, help='location to write and run executable (default: temp dir)')
    parser.add_argument('-s', '--skip-compile', dest='skip_compile', action='store_const', const=True, default=False, help='skip compilation if executable exists')
    parser.add_argument('--skip-build', dest='skip_build', action='store_const', const=True, default=False, help='skip building the compiler')
    parser.add_argument('--autograder', dest='autograder', action='store_const', const=True, default=False, help='only for use by autograder')
    return parser.parse_args()

if __name__ == '__main__':
    args = parse_args()

    if args.verbose:
        verbose = True
        logging.basicConfig(level=logging.DEBUG, format='%(message)s')

    chdir_project_root()

    testsets = args.testsets
    test_filter = args.filter
    list_tests = args.list_tests
    show_expected = args.show_expected
    debug_mode = args.debug
    override_opt = args.opt
    computer_readable = args.autograder
    benchmark = args.benchmark
    additional_args = args.args
    autograder = args.autograder
    skip_build = args.skip_build

    if args.run_dir is not None:
        persistent_run_dir = os.path.abspath(args.run_dir)

    if args.skip_compile and persistent_run_dir is not None:
        exec_fname = os.path.join(persistent_run_dir, EXEC_FILE_NAME)
        skip_compile = os.path.exists(exec_fname)

    if list_tests or show_expected:
        skip_compile = True

    check_arch()
    do_requested_tests()
