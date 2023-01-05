import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Performance extends Thread {

  public static final boolean FAST = false;

  public static final int THREADS = FAST ? 3 : 6;

  public static final int FIB_MAX = FAST ? 39 : 45;
  public static final int HASH_MAX = FAST ? 500_000 : 1_000_000;
  public static final int WRITE_MAX = FAST ? 2_000 : 10_000;
  public static final int MEMORY_MAX = FAST ? 1_000 : 2_000;


  public static void main(String[] args) {

    List<Test> running = new ArrayList<>(THREADS);

    System.out.println("----------------------------------------------------------------------------------");
    System.out.println("fast="+FAST);
    System.out.println("----------------------------------------------------------------------------------");

    for (int i = 0; i < THREADS; i++) {
      final Test test = new Fibonacci(FIB_MAX);
      test.setNumber(i);
      running.add(test);
      test.start();
    }

    waitForFinish(running);

    System.out.println("----------------------------------------------------------------------------------");

    for (int i = 0; i < THREADS; i++) {
      final Test test = new Hash(HASH_MAX);
      test.setNumber(i);
      running.add(test);
      test.start();
    }

    waitForFinish(running);

    System.out.println("----------------------------------------------------------------------------------");

    for (int i = 0; i < THREADS; i++) {
      final Test test = new Write(WRITE_MAX);
      test.setNumber(i);
      running.add(test);
      test.start();
    }

    waitForFinish(running);

    System.out.println("----------------------------------------------------------------------------------");

    for (int i = 0; i < THREADS; i++) {
      final Test test = new Memory(MEMORY_MAX);
      test.setNumber(i);
      running.add(test);
      test.start();
    }

    waitForFinish(running);

    System.out.println("----------------------------------------------------------------------------------");
  }

  private static void waitForFinish(List<Test> running) {
    long sum = 0L;
    long max = 0L;
    while (!running.isEmpty()) {
      for (Test thread : running) {
        if (!thread.isAlive()) {
          running.remove(thread);
          final long duration = thread.getDuration();
          sum += duration;
          max = Math.max(max, duration);
          break; // avoid concurrent modification exception
        }
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }
    System.out.printf("Result: Ø=%f max=%f%n", sum / THREADS / (double) 1_000_000_000, max / (double) 1_000_000_000);
  }

  private static abstract class Test extends Thread {

    private long start;
    private long stop;

    public void setNumber(int i) {
      setName(getClass().getSimpleName() + "-" + i);
    }

    @Override
    public void run() {
      System.out.printf("starting: '%s'\n", getName());
      start = System.nanoTime();
      test();
      stop = System.nanoTime();
    }

    abstract protected void test();

    public long getDuration() {
      return stop - start;
    }
  }

  /**
   * Integer math test
   */
  private static class Fibonacci extends Test {

    private int max;

    public Fibonacci(int max) {
      this.max = max;
    }

    public void test() {
      for (int n = 1; n <= max; n++) {
        final long fib = fib(n);
//        System.out.printf("fib(%d) = %d%n", n, fib);
      }
    }

    private long fib(int n) {
      if (n <= 2) {
        return 1;
      }
      return fib(n - 1) + fib(n - 2);
    }

  }

  /**
   * Hash test
   */
  private static class Hash extends Test {

    private int max;

    public Hash(int max) {
      this.max = max;
    }

    public void test() {
      HashMap<Integer, String> map = new HashMap<>();
      for (int n = 1; n <= max; n++) {
        map.put(n, "" + -n);
      }
      for (int n = 1; n <= max; n++) {
        final String s = map.get(n);
        if (!s.equals("-" + n)) {
          throw new RuntimeException("" + n);
        }
      }
//      System.out.print("++" + map.size());
    }
  }

  /**
   * File writing test
   */
  private static class Write extends Test {

    private int max;

    public Write(int max) {
      this.max = max;
    }

    public void test() {

      try {
        for (int n = 1; n <= max; n++) {
          File file = File.createTempFile(getName() + "-" + n, null);
//          System.out.println("path=" + file.getCanonicalPath());
          file.deleteOnExit();
          PrintWriter writer = new PrintWriter(file);
          for (int i = 0; i < 100; i++) {
            writer.println("A quick brown fox jumps over the lazy dog... " + i);
          }
          writer.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Memory test. Creates N times an array of 1 MB size.
   */
  private static class Memory extends Test {

    private int max;

    public Memory(int max) {
      this.max = max;
    }

    public void test() {

        for (int n = 1; n <= max; n++) {
          char[] data = new char[1_000_000];
          data[74_843] = 'X';
          char x = data[452_345];
        }
    }
  }

}
