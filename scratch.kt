fun main() {
    val raw = "**Ggvgg**"
    val MarkdownPattern: Regex = Regex(
        """\*\*(?<bold>[^*\n]+?)\*\*|==(?<mark>[^=\n]+?)==|\*(?<italic>[^*\n]+?)\*"""
    )
    val matches = MarkdownPattern.findAll(raw).toList()
    val n = raw.length
    val isMarker = BooleanArray(n)
    
    for (match in matches) {
        val bold = match.groups["bold"]
        val mark = match.groups["mark"]
        val italic = match.groups["italic"]
        val mLen: Int
        when {
            bold != null -> { mLen = 2 }
            mark != null -> { mLen = 2 }
            italic != null -> { mLen = 1 }
            else -> continue
        }
        val matchStart = match.range.first
        val matchEnd = match.range.last + 1
        println("matchStart: $matchStart, matchEnd: $matchEnd, mLen: $mLen")
        for (i in matchStart until matchStart + mLen) isMarker[i] = true
        for (i in matchEnd - mLen until matchEnd) isMarker[i] = true
    }
    
    val displayBuilder = StringBuilder(n)
    for (i in 0 until n) {
        if (!isMarker[i]) {
            displayBuilder.append(raw[i])
        }
    }
    println("Display: '$displayBuilder'")
}
