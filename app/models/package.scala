package object models {
  def descInsert[T](v: Vector[T], t: T)(implicit ord: Ordering[T]): Vector[T] = {
    // slight-optimization opportunity: binary search (instead of linear). effort started in binary_search_insert branch
    val (prefix, suffix) = v.partition(ord.gteq(_, t))

    (prefix :+ t) ++ suffix
  }
}
