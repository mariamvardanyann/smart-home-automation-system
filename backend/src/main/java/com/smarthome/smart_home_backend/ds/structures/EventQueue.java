package com.smarthome.smart_home_backend.ds.structures;

import com.smarthome.smart_home_backend.ds.model.*;

public class EventQueue {

    private static class Node {
        SensorEvent data;
        Node next;

        Node(SensorEvent data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node front;
    private Node rear;
    private int size;

    public EventQueue() {
        this.front = null;
        this.rear = null;
        this.size = 0;
    }

    public void enqueue(SensorEvent event) {
        Node newNode = new Node(event);
        if (rear != null) {
            rear.next = newNode;
        }
        rear = newNode;
        size++;

        if (front == null) {
            front = newNode;
        }
    }

    public SensorEvent dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("Queue is empty. Cannot dequeue.");
        }

        SensorEvent event = front.data;
        front = front.next;

        if (front == null) {
            rear = null;
        }

        size--;
        return event;
    }

    public SensorEvent peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Queue is empty. Cannot peek.");
        }

        return front.data;
    }

    public boolean isEmpty() {
        return front == null;
    }

    public int size() {
        return size;
    }

    public void printQueue() {
        Node current = front;

        if (current == null) {
            System.out.println("Event queue is empty.");
            return;
        }

        System.out.println("Event Queue:");
        while (current != null) {
            System.out.println(current.data);
            current = current.next;
        }
    }
}