interface Node {
    val text: String
}

class Program(val statements: List<Statement>): Node {
    override val text: String
        get() = statements.joinToString(separator = "") { it.text }
}

// Statement
interface Statement : Node {
    val token: Token
}

data class LetStatement(
        override val token: Token,
        val name: IdentifierExpression,
        val value: Expression
) : Statement {
    override val text
        get() = "${token.literal} ${name.text} = ${value.text};"
}

data class ReturnStatement(override val token: Token, val value: Expression) : Statement {
    override val text
        get() = "${token.literal} ${value.text};"
}

data class ExpressionStatement(override val token: Token, val expression: Expression) : Statement {
    override val text
        get() = expression.text
}

data class BlockStatement(override val token: Token, val statements: List<Statement>) : Statement {
    override val text: String
        get() = buildString {
            append(statements.joinToString(separator = "; ", postfix = ";") { it.text })
        }
}

// Expression
interface Expression : Node {
    val token: Token
}

data class IdentifierExpression(override val token: Token, val value: String) : Expression {
    override val text
        get() = token.literal
}

data class IntegerExpression(override val token: Token, val value: Int) : Expression {
    override val text
        get() = token.literal
}

data class BoolExpression(override val token: Token, val value: Boolean) : Expression {
    override val text
        get() = token.literal
}

data class StringExpression(override val token: Token, val value: String): Expression {
    override val text
        get() = token.literal
}

data class PrefixExpression(override val token: Token, val operator: String, val right: Expression) : Expression {
    override val text
        get() = "($operator${right.text})"
}

data class BinaryExpression(
        override val token: Token,
        val left: Expression,
        val operator: String,
        val right: Expression
) : Expression {
    override val text
        get() = "(${left.text} $operator ${right.text})"
}

data class IfExpression(
        override val token: Token,
        val condition: Expression,
        val consequence: BlockStatement,
        val alternative: BlockStatement?
) : Expression {
    override val text: String
        get() = buildString {
            append("if ${condition.text} ${consequence.text}")
            if (alternative != null) append(" else ${alternative.text}")
        }
}

data class FunctionExpression(
        override val token: Token,
        val parameters: List<IdentifierExpression>,
        val body: BlockStatement
) : Expression {
    override val text: String
        get() = buildString {
            append("${token.literal}(")
            append(parameters.joinToString(separator = ", ") { it.text })
            append(") ${body.text}")
        }
}

data class CallExpression(
        override val token: Token,
        val function: Expression,
        val arguments: List<Expression>
) : Expression {
    override val text: String
        get() = buildString {
            append("${function.text}(")
            append(arguments.joinToString(separator = ", ") { it.text })
            append(")")
        }
}

data class ArrayExpression(override val token: Token, val elements: List<Expression>) : Expression {
    override val text: String
        get() = buildString {
            append("[")
            append(elements.joinToString(separator = ", ") { it.text })
            append("]")
        }
}

data class HashExpression(override val token: Token, val pairs: List<Pair<Expression, Expression>>) : Expression {
    override val text: String
        get() = buildString {
            append("{")
            append(pairs.joinToString(separator = ", ") { "${it.first.text}:${it.second.text}" })
            append("}")
        }
}

data class IndexExpression(override val token: Token, val left: Expression, val index: Expression) : Expression {
    override val text: String
        get() = "(${left.text}[${index.text}])"
}