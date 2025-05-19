package net.sebyte

import com.strumenta.antlrkotlin.parsers.generated.SQLiteLexer
import com.strumenta.antlrkotlin.parsers.generated.SQLiteParser
import com.strumenta.antlrkotlin.parsers.generated.SQLiteParserBaseVisitor
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.TokenStreamRewriter
import org.antlr.v4.kotlinruntime.misc.Interval
import org.antlr.v4.kotlinruntime.tree.SyntaxTree
import java.io.File

// https://people.inf.ethz.ch/suz/publications/icse06-hdd.pdf

private class Listener(
    private val charStream: CharStream,
    tokens: CommonTokenStream
) : SQLiteParserBaseVisitor<Unit>() {
    private val rewriter = TokenStreamRewriter(tokens)
    override fun defaultResult() {}

    override fun visitCreate_table_stmt(ctx: SQLiteParser.Create_table_stmtContext) {
        super.visitCreate_table_stmt(ctx)
        val interval = Interval(ctx.start!!.startIndex, ctx.stop!!.stopIndex)
        println(charStream.getText(interval))
    }
}

fun testFile(file: File) = ProcessBuilder("./check.sh", file.absolutePath)
    .start()
    .waitFor() == 0

fun main() {
    val file = File("./queries/query1/original_test.sql")
    val stream = CharStreams.fromFileName(file.absolutePath)
    val lexer = SQLiteLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = SQLiteParser(tokens)
    val parse = parser.parse()
    if (parser.numberOfSyntaxErrors > 0) error("Syntax error!")

    val useless = mutableMapOf<SyntaxTree, Boolean>()
    fun isUseless(node: SyntaxTree) = useless.getOrDefault(node, false)

    fun maxLevel(node: SyntaxTree): Int =
        if (node.childCount == 0) 1
        else List(node.childCount) { node.getChild(it) }
            .maxOf { maxLevel(it as SyntaxTree) } + 1

    fun tagNodes(node: SyntaxTree, level: Int): List<SyntaxTree> =
        if (isUseless(node)) emptyList()
        else if (node.childCount == 0) listOf(node)
        else List(node.childCount) { node.getChild(it) }
            .map { it as SyntaxTree }
            .filter { !isUseless(it) }
            .flatMap {
                if (level > 0) tagNodes(it, level - 1)
                else listOf(it)
            }

    fun test(nodes: List<SyntaxTree>): Boolean {
        val sql = buildString {
            for (node in nodes) {
                append(stream.getText(node.sourceInterval))
            }
        }
        println("Testing: $sql")
        val file = File.createTempFile("reduced_query", ".sql")
        file.writeText(sql)
        val success = testFile(file)
        println("Success: $success")
        return success
    }

    fun ddmin(nodes: List<SyntaxTree>, n0: Int = 2): List<SyntaxTree> {
        for (n in n0..nodes.size) {
            val deltas: List<List<SyntaxTree>> = List(n) {
                val size = nodes.size / n
                nodes.subList(it * size, (it + 1) * size)
            }

            for ((d, delta) in deltas.withIndex()) {
                val complement = deltas.filterIndexed { i, _ -> i != d }.flatten()
                if (test(delta)) {
                    println("Delta!")
                    return ddmin(delta)
                } else if (test(complement)) {
                    println("Complement")
                    return ddmin(complement, n-1)
                }
            }
        }
        println("Done!")
        return nodes
    }

    fun prune(nodes: List<SyntaxTree>, minconfig: List<SyntaxTree>) {
        for (node in nodes) {
            if (node in minconfig) continue
            useless[node] = true
        }
    }

    var nodes = tagNodes(parse, 0)
    for (level in 0..maxLevel(parse)) {
        println("Level $level")
        val minconfig = ddmin(nodes)
        println("Minconfig: ${minconfig.size}")
        prune(nodes, minconfig)
        nodes = tagNodes(parse, level)
        println("Nodes: ${nodes.size}")
    }
    for(node in nodes) print(stream.getText(node.sourceInterval))
}