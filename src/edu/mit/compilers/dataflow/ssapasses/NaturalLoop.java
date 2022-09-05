package edu.mit.compilers.dataflow.ssapasses;

import edu.mit.compilers.cfg.BasicBlock;

public class NaturalLoop {
    private BasicBlock preHeader;

    private BasicBlock head;

    private BasicBlock body;

    private BasicBlock tail;

    private BasicBlock exit;

    public NaturalLoop(BasicBlock head, BasicBlock body, BasicBlock tail) {
        this.head = head;
        this.body = body;
        this.tail = tail;
    }

    public void setBody(BasicBlock body) {
        this.body = body;
    }

    public void setHead(BasicBlock head) {
        this.head = head;
    }

    public void setTail(BasicBlock tail) {
        this.tail = tail;
    }

    public void setExit(BasicBlock exit) {
        this.exit = exit;
    }

    public void setPreHeader(BasicBlock preHeader) {
        this.preHeader = preHeader;
    }

    public BasicBlock getBody() {
        return body;
    }

    public BasicBlock getHead() {
        return head;
    }

    public BasicBlock getPreHeader() {
        return preHeader;
    }

    public BasicBlock getTail() {
        return tail;
    }

    public BasicBlock getExit() {
        return exit;
    }
}
