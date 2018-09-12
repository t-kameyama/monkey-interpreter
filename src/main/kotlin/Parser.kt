private typealias prefixParseFn = () -> Expression?

private typealias binaryParseFn = (Expression) -> Expression?

private enum class Precedence {
    LOWEST,
    EQUALS,
    LESSGREATER,
    SUM,
    PRODUCT,
    PREFIX,
    CALL,
    INDEX
}

private val precedences = mapOf(
        TokenType.EQ to Precedence.EQUALS,
        TokenType.NOT_EQ to Precedence.EQUALS,
        TokenType.LT to Precedence.LESSGREATER,
        TokenType.GT to Precedence.LESSGREATER,
        TokenType.PLUS to Precedence.SUM,
        TokenType.MINUS to Precedence.SUM,
        TokenType.ASTERISK to Precedence.PRODUCT,
        TokenType.SLASH to Precedence.PRODUCT,
        TokenType.LPAREN to Precedence.CALL,
        TokenType.LBRACKET to Precedence.INDEX
)

class Parser(private val lexer: Lexer) {

    constructor(input: String): this(Lexer(input))

    private var curToken: Token? = null
    private var peekToken: Token? = null
    private val errors = mutableListOf<String>()
    private val prefixParseFns = mapOf<TokenType, prefixParseFn>(
            TokenType.IDENT to ::parseIdentifierExpression,
            TokenType.INT to ::parseIntegerExpression,
            TokenType.TRUE to ::parseBoolExpression,
            TokenType.FALSE to ::parseBoolExpression,
            TokenType.STRING to ::parseStringExpression,
            TokenType.BANG to ::parsePrefixExpression,
            TokenType.MINUS to ::parsePrefixExpression,
            TokenType.LPAREN to ::parseGroupedExpression,
            TokenType.IF to ::parseIfExpression,
            TokenType.FUNCTION to ::parseFunctionExpression,
            TokenType.LBRACKET to ::parseArrayExpression,
            TokenType.LBRACE to ::parseHashExpression
    )
    private val binaryParseFns = mapOf<TokenType, binaryParseFn>(
            TokenType.PLUS to ::parseBinaryExpression,
            TokenType.MINUS to ::parseBinaryExpression,
            TokenType.ASTERISK to ::parseBinaryExpression,
            TokenType.SLASH to ::parseBinaryExpression,
            TokenType.EQ to ::parseBinaryExpression,
            TokenType.NOT_EQ to ::parseBinaryExpression,
            TokenType.LT to ::parseBinaryExpression,
            TokenType.GT to ::parseBinaryExpression,
            TokenType.LPAREN to ::parseCallExpression,
            TokenType.LBRACKET to ::parseIndexExpression
    )

    init {
        nextToken()
        nextToken()
    }

    fun parseProgram(): Program {
        val statements = mutableListOf<Statement>()
        while (!curTokenIs(TokenType.EOF)) {
            parseStatement()?.also { statements.add(it) }
            nextToken()
        }
        return Program(statements)
    }

    fun errors(): List<String> = errors

    private fun nextToken() {
        curToken = peekToken
        peekToken = lexer.nextToken()
    }

    private fun parseStatement(): Statement? {
        return when (curToken?.type) {
            TokenType.LET -> parseLetStatement()
            TokenType.RETURN -> parseReturnStatement()
            else -> parseExpressionStatement()
        }
    }

    private fun parseLetStatement(): LetStatement? {
        val token = curToken ?: return null

        if (!expectPeek(TokenType.IDENT)) return null
        val name = curToken?.let { IdentifierExpression(it, it.literal) } ?: return null

        if (!expectPeek(TokenType.ASSIGN)) return null
        nextToken()
        val expression = parseExpression(Precedence.LOWEST) ?: return null

        if (peekTokenIs(TokenType.SEMICOLON)) nextToken()
        return LetStatement(token, name, expression)
    }

    private fun parseReturnStatement(): ReturnStatement? {
        val token = curToken ?: return null
        nextToken()
        val expression = parseExpression(Precedence.LOWEST) ?: return null

        if (peekTokenIs(TokenType.SEMICOLON)) nextToken()
        return ReturnStatement(token, expression)
    }

    private fun parseExpressionStatement(): ExpressionStatement? {
        val token = curToken ?: return null
        val expression = parseExpression(Precedence.LOWEST) ?: return null
        val stmt = ExpressionStatement(token, expression)
        if (peekTokenIs(TokenType.SEMICOLON)) {
            nextToken()
        }
        return stmt
    }

    private fun parseBlockStatement(): BlockStatement? {
        val token = curToken ?: return null
        val statements = mutableListOf<Statement>()
        nextToken()
        while (!curTokenIs(TokenType.RBRACE) && !curTokenIs(TokenType.EOF)) {
            val stmt = parseStatement()
            if (stmt != null) statements.add(stmt)
            nextToken()
        }
        return BlockStatement(token, statements)
    }

    private fun parseExpression(precedence: Precedence): Expression? {
        val token = curToken ?: return null
        var left = prefixParseFns[token.type]?.invoke() ?: run {
            errors.add("no prefix parse function for ${token.type.value} found")
            return null
        } ?: return null

        while (!peekTokenIs(TokenType.SEMICOLON) && precedence < peekPrecedence()) {
            val binary = binaryParseFns[peekToken?.type] ?: return left
            nextToken()
            left = binary(left) ?: return left
        }
        return left
    }

    private fun parseIdentifierExpression(): Expression? {
        val token = curToken ?: return null
        return IdentifierExpression(token, token.literal)
    }

    private fun parseIntegerExpression(): Expression? {
        val token = curToken ?: return null
        val value = token.literal.toIntOrNull() ?: run {
            errors.add("could not parse ${token.literal} as integer")
            return null
        }
        return IntegerExpression(token, value)
    }

    private fun parseBoolExpression(): Expression? {
        val token = curToken ?: return null
        return BoolExpression(token, curTokenIs(TokenType.TRUE))
    }

    private fun parseStringExpression(): Expression? {
        val token = curToken ?: return null
        return StringExpression(token, token.literal)
    }

    private fun parsePrefixExpression(): Expression? {
        val token = curToken ?: return null
        val operator = token.literal

        nextToken()
        val right = parseExpression(Precedence.PREFIX) ?: return null

        return PrefixExpression(token, operator, right)
    }

    private fun parseBinaryExpression(left: Expression): Expression? {
        val token = curToken ?: return null
        val operator = token.literal
        val precedence = curPrecedence()
        nextToken()
        val right = parseExpression(precedence) ?: return null
        return BinaryExpression(
                token = token,
                left = left,
                operator = operator,
                right = right
        )
    }

    private fun parseGroupedExpression(): Expression? {
        nextToken()
        val exp = parseExpression(Precedence.LOWEST)
        if (!expectPeek(TokenType.RPAREN)) return null
        return exp
    }

    private fun parseIfExpression(): Expression? {
        val token = curToken ?: return null
        if (!expectPeek(TokenType.LPAREN)) return null
        nextToken()
        val condition = parseExpression(Precedence.LOWEST) ?: return null
        if (!expectPeek(TokenType.RPAREN)) return null
        if (!expectPeek(TokenType.LBRACE)) return null
        val consequence = parseBlockStatement() ?: return null
        val alternative = if (peekTokenIs(TokenType.ELSE)) {
            nextToken()
            if (!expectPeek(TokenType.LBRACE)) return null
            parseBlockStatement()
        } else {
            null
        }
        return IfExpression(token = token, condition = condition, consequence = consequence, alternative = alternative)
    }

    private fun parseFunctionExpression(): Expression? {
        val token = curToken ?: return null
        if (!expectPeek(TokenType.LPAREN)) return null

        val parameters = parseFunctionParameters() ?: return null
        if (!expectPeek(TokenType.LBRACE)) return null

        val body = parseBlockStatement() ?: return null

        return FunctionExpression(token, parameters, body)
    }

    private fun parseFunctionParameters(): List<IdentifierExpression>? {
        val parameters = mutableListOf<IdentifierExpression>()
        if (peekTokenIs(TokenType.RPAREN)) {
            nextToken()
            return parameters
        }

        nextToken()
        curToken?.let { parameters.add(IdentifierExpression(it, it.literal)) }
        while (peekTokenIs(TokenType.COMMA)) {
            nextToken()
            nextToken()
            curToken?.let { parameters.add(IdentifierExpression(it, it.literal)) }
        }
        if (!expectPeek(TokenType.RPAREN)) return null

        return parameters
    }

    private fun parseExpressionList(end: TokenType): List<Expression>? {
        val arguments = mutableListOf<Expression>()
        if (peekTokenIs(end)) {
            nextToken()
            return arguments
        }
        nextToken()
        parseExpression(Precedence.LOWEST)?.let { arguments.add(it) }
        while (peekTokenIs(TokenType.COMMA)) {
            nextToken()
            nextToken()
            parseExpression(Precedence.LOWEST)?.let { arguments.add(it) }
        }
        if (!expectPeek(end)) return null
        return arguments
    }

    private fun parseCallExpression(function: Expression): Expression? {
        val token = curToken ?: return null
        val arguments = parseExpressionList(TokenType.RPAREN) ?: return null
        return CallExpression(token, function, arguments)
    }

    private fun parseArrayExpression(): Expression? {
        val token = curToken ?: return null
        val elements = parseExpressionList(TokenType.RBRACKET) ?: return null
        return ArrayExpression(token, elements)
    }

    private fun parseIndexExpression(left: Expression): Expression? {
        val token = curToken ?: return null
        nextToken()
        val index = parseExpression(Precedence.LOWEST) ?: return null
        if (!expectPeek(TokenType.RBRACKET)) return null
        return IndexExpression(token, left = left, index = index)
    }

    private fun parseHashExpression(): Expression? {
        val token = curToken ?: return null
        val pairs = mutableListOf<Pair<Expression, Expression>>()
        while (!peekTokenIs(TokenType.RBRACE)) {
            nextToken()
            val key = parseExpression(Precedence.LOWEST) ?: return null
            if (!expectPeek(TokenType.COLON)) return null

            nextToken()
            val value = parseExpression(Precedence.LOWEST) ?: return null

            pairs.add(key to value)
            if (!peekTokenIs(TokenType.RBRACE) && !expectPeek(TokenType.COMMA)) return null
        }

        if (!expectPeek(TokenType.RBRACE)) return null

        return HashExpression(token, pairs)
    }

    private fun curTokenIs(type: TokenType): Boolean = type == curToken?.type

    private fun peekTokenIs(type: TokenType): Boolean = type == peekToken?.type

    private fun expectPeek(type: TokenType): Boolean {
        return if (peekTokenIs(type)) {
            nextToken()
            true
        } else {
            errors.add("expected next token to be ${type.value}, got ${peekToken?.type?.value} instead")
            false
        }
    }

    private fun curPrecedence(): Precedence {
        return curToken?.let { precedences[it.type] } ?: Precedence.LOWEST
    }

    private fun peekPrecedence(): Precedence {
        return peekToken?.let { precedences[it.type] } ?: Precedence.LOWEST
    }
}