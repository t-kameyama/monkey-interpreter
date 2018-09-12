data class Token(val type: TokenType, val literal: String)

enum class TokenType(val value: String) {
    ILLEGAL("ILLEGAL"),
    EOF("EOF"),

    FUNCTION("FUNCTION"),
    LET("LET"),
    IF("IF"),
    ELSE("ELSE"),
    RETURN("RETURN"),
    TRUE("TRUE"),
    FALSE("FALSE"),

    IDENT("IDENT"),
    INT("INT"),
    STRING("STRING"),

    COMMA(","),
    SEMICOLON(";"),
    COLON(":"),
    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),
    LBRACKET("["),
    RBRACKET("]"),

    ASSIGN("="),
    PLUS("+"),
    MINUS("-"),
    ASTERISK("*"),
    SLASH("/"),
    EQ("=="),
    NOT_EQ("!="),
    LT("<"),
    GT(">"),
    BANG("!")
    ;

    companion object {
        private val keywords = mapOf(
                "fn" to FUNCTION,
                "let" to LET,
                "if" to IF,
                "else" to ELSE,
                "return" to RETURN,
                "true" to TRUE,
                "false" to FALSE
        )

        fun lookupIdent(ident: String): TokenType {
            return keywords[ident] ?: IDENT
        }
    }
}