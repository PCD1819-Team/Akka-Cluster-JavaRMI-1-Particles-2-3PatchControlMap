package it.unibo.isi.pcd.utils;

import java.util.LinkedList;
import java.util.Queue;

public class ObservableQueue<T> {

  private final Queue<T> queue = new LinkedList<>();

  public synchronized T element() {
    while (this.queue.isEmpty()) {
      try {
        this.wait();
      } catch (final InterruptedException e) {
      }
    }
    return this.queue.element();
  }

  public synchronized boolean offer(final T e) {
    this.notifyAll();
    return this.queue.offer(e);
  }

  public synchronized T peek() {
    while (this.queue.isEmpty()) {
      try {
        this.wait();
      } catch (final InterruptedException e) {
      }
    }
    return this.queue.peek();
  }

  public synchronized void clear() {
    this.queue.clear();
  }

  public synchronized T poll() {
    while (this.queue.isEmpty()) {
      try {
        this.wait();
      } catch (final InterruptedException e) {
      }
    }
    return this.queue.poll();
  }

  public synchronized T remove() {
    while (this.queue.isEmpty()) {
      try {
        this.wait();
      } catch (final InterruptedException e) {
      }
    }
    return this.queue.remove();
  }

  public synchronized int getSize() {
    return this.queue.size();
  }

}
