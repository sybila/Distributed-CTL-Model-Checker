package cz.muni.fi.modelchecker

public fun <T: Any> T.repeat(n: Int): List<T> = (1..n).map { this }
public fun <I, T: Iterable<I>> T.flatRepeat(n: Int): List<I> = (1..n).flatMap { this }