package org.example.infra.graph

interface OrientedGraph<N, W>
{
    fun addNode(x : N)
    fun addEdge(from : N, to : N, weight: W?)
}

data class TailEdge<N, W>(
    val node : N,
    val weight: W
)

