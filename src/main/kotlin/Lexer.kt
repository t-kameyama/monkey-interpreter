import TokenType.*

class Lexer(private val input: String) {

    private var position: Int = 0
    private var readPosition: Int = 0
    private var ch: Char? = null

    init {
        readChar()
    }

    fun nextToken(): Token {
        skipWhitespace()

        val ch = this.ch

        if (ch.isLetter()) {
            val literal = read { it.isLetter() }
            return Token(TokenType.lookupIdent(literal), literal)
        }
        if (ch.isDigit()) {
            val literal = read { it.isDigit() }
            return Token(INT, literal)
        }

        val token = when (ch) {
            null -> Token(EOF, "")
            ',' -> Token(COMMA, "$ch")
            ';' -> Token(SEMICOLON, "$ch")
            ':' -> Token(COLON, "$ch")
            '(' -> Token(LPAREN, "$ch")
            ')' -> Token(RPAREN, "$ch")
            '{' -> Token(LBRACE, "$ch")
            '}' -> Token(RBRACE, "$ch")
            '[' -> Token(LBRACKET, "$ch")
            ']' -> Token(RBRACKET, "$ch")
            '=' -> {
                val peekChar = peekChar()
                if (peekChar == '=') {
                    readChar()
                    Token(EQ, "$ch$peekChar")
                } else {
                    Token(ASSIGN, "$ch")
                }
            }
            '+' -> Token(PLUS, "$ch")
            '-' -> Token(MINUS, "$ch")
            '!' -> {
                val peekChar = peekChar()
                if (peekChar == '=') {
                    readChar()
                    Token(NOT_EQ, "$ch$peekChar")
                } else {
                    Token(BANG, "$ch")
                }
            }
            '/' -> Token(SLASH, "$ch")
            '*' -> Token(ASTERISK, "$ch")
            '<' -> Token(LT, "$ch")
            '>' -> Token(GT, "$ch")
            '"' -> Token(STRING, readString())
            else -> Token(ILLEGAL, "$ch")
        }
        readChar()
        return token
    }

    private fun readString(): String {
        val position = position + 1
        while (true) {
            readChar()
            if (ch == '"' || ch == null) {
                break
            }
        }
        return input.substring(position, this.position)
    }

    private fun readChar() {
        ch = input.getOrNull(readPosition)
        position = readPosition
        readPosition += 1
    }

    private fun peekChar(): Char? {
        return input.getOrNull(readPosition)
    }

    private fun skipWhitespace() {
        while (ch in listOf(' ', '\t', '\n', '\r')) readChar()
    }

    private fun read(p: (Char?) -> Boolean): String {
        val position = this.position
        while (p(ch)) readChar()
        return input.substring(position, this.position)
    }

    private fun Char?.isLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this == '_'

    private fun Char?.isDigit(): Boolean = this in '0'..'9'
}
