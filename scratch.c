#include <stdio.h>

int array[10];

int main (void) {
  int j;
  printf ( "below output should be 18 16 14 12 10 8 6 4 2 0\n" );
  for ( j = 0; j < 10; j += 1 ) {
    array[j] = j*2;
  }
  for ( j = 0; j < 10; j += 1 ) {
    printf ( "%d ", array[9 - j] );
  }
  printf ( "\n" );
  return 0;
}

