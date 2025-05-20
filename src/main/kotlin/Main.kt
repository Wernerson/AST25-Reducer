package net.sebyte

import com.strumenta.antlrkotlin.parsers.generated.SQLiteLexer
import com.strumenta.antlrkotlin.parsers.generated.SQLiteParser
import com.strumenta.antlrkotlin.parsers.generated.SQLiteParserBaseVisitor
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Token
import org.antlr.v4.kotlinruntime.tree.RuleNode
import org.antlr.v4.kotlinruntime.tree.SyntaxTree
import org.antlr.v4.kotlinruntime.tree.TerminalNode
import org.antlr.v4.kotlinruntime.tree.Tree
import java.io.File
import kotlin.math.max

private class QueryPrinter(
    private val needMap: Map<Tree, Boolean>
) : SQLiteParserBaseVisitor<String>() {

    private fun needed(node: Tree) = needMap.getOrDefault(node, true)

    override fun defaultResult() = ""

    override fun visitChildren(node: RuleNode): String = buildString {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (!needed(child)) continue
            append(child.accept(this@QueryPrinter).trim())
            append(" ")
        }
    }

    override fun visitTerminal(node: TerminalNode) =
        if (node.symbol.type == Token.EOF) ""
        else if (needed(node)) node.text
        else ""
}

fun testFile(file: File) = ProcessBuilder("./check.sh", file.absolutePath)
    .start()
    .waitFor() == 0

fun main() {
    val file = File("./queries/query20/original_test.sql")
    val stream = CharStreams.fromFileName(file.absolutePath)
    val lexer = SQLiteLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = SQLiteParser(tokens)
    val parse = parser.parse()
    if (parser.numberOfSyntaxErrors > 0) error("Syntax error!")

    val needMap = mutableMapOf<Tree, Boolean>()
    fun needed(node: Tree) = needMap.getOrDefault(node, true)

    fun getNodesOfLevel(node: SyntaxTree, level: Int): List<SyntaxTree> =
        if (!needed(node)) emptyList()
        else if (level == 0) listOf(node)
        else if (node.childCount == 0) emptyList()
        else List(node.childCount) { node.getChild(it) }
            .map { it as SyntaxTree }
            .filter { needed(it) }
            .flatMap { getNodesOfLevel(it, level - 1) }

    fun query(): String = QueryPrinter(needMap).visit(parse)

    fun test(): Boolean {
        val sql = query()
        val file = File.createTempFile("reduced_query", ".sql")
        file.writeText(sql)
        return testFile(file)
    }

    fun ddmin(nodes: List<SyntaxTree>, n0: Int = 2): List<SyntaxTree> {
        for (n in n0..nodes.size) {
            val deltas: List<List<SyntaxTree>> = List(n) {
                val size = nodes.size / n
                nodes.subList(it * size, (it + 1) * size)
            }

            for ((d, delta) in deltas.withIndex()) {
                for (node in nodes) needMap[node] = false
                for (node in delta) needMap[node] = true
                if (test()) return ddmin(delta)

                val complement = deltas.filterIndexed { i, _ -> i != d }.flatten()
                for (node in nodes) needMap[node] = false
                for (node in complement) needMap[node] = true
                if (test()) return ddmin(complement, n - 1)
            }
        }
        return nodes
    }

    fun prune(nodes: List<SyntaxTree>, minconfig: List<SyntaxTree>) {
        for (node in nodes) needMap[node] = false
        for (node in minconfig) needMap[node] = true
    }

    var nodes = getNodesOfLevel(parse, 0)
    var level = 0
    while (nodes.isNotEmpty()) {
        println("Level: $level")
        println("Nodes: ${nodes.size}")
        val minconfig = ddmin(nodes)
        println("Minconfig: ${minconfig.size}")
        prune(nodes, minconfig)
        ++level
        nodes = getNodesOfLevel(parse, level)
    }

    println(query())
}