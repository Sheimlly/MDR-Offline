package com.mdr.offline.data

class Queue<T> {
    private val items = ArrayDeque<T>()

    fun enqueue(item: T) = items.addLast(item)

    fun dequeue(): T? = items.removeFirstOrNull()

    fun peek(): T? = items.firstOrNull()

    fun isEmpty(): Boolean = items.isEmpty()
}