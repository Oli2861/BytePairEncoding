package BytePairEncoding

fun main() {
    val encoder = BytePairEncoding()
    val corpus = "This is some example text. It is not very long, but it is enough to demonstrate the algorithm."
    encoder.train(corpus, 10)

    val encoded = encoder.inferenceEncode("This is some example text.")
    println(encoded)

    val decoded = encoder.inferenceDecode(encoded)
    println(decoded)
}

class BytePairEncoding {
    private var vocab = mutableMapOf<String, Int>()
    private val mergeTable = mutableMapOf<String, String>()

    companion object {
        private const val endOfWordToken = "_"
        private val endOfWordTokenRegex = endOfWordToken.toRegex()
        private val notEndOfWordTokenRegex = "[^$endOfWordToken]".toRegex()

        private fun countSymbolPairs(vocab: Map<String, Int>): Map<String, Int> {
            //     More efficient: 
            //     2.1 Count words (in vocab)
            //     2.2 Based on words, count pairs of symbols
            val pairs = mutableMapOf<String, Int>()
            vocab.forEach { (word, wordCount) ->
                val counts: Map<String, Int> = countSymbolPairsInWord(word)
                counts.forEach { (pair, count) ->
                    val overallCount = wordCount * count
                    pairs[pair] = pairs[pair]?.plus(overallCount) ?: overallCount
                }
            }
            return pairs
        }

        private fun countSymbolPairsInWord(word: String): Map<String, Int> {
            val bigramsOfWord = mutableMapOf<String, Int>()
            var n = 0
            while (n < word.length - 1) {
                println("Getting from index $n")
                val (first, firstEndIndex) = getFromIndex(word, n)
                n = firstEndIndex + 1
                if (n >= word.length) break
                println("Getting second from index $n (length ${word.length})")
                val (second, _) = getFromIndex(word, n)
                val bigram = "$first$second"
                bigramsOfWord[bigram] = bigramsOfWord[bigram]?.plus(1) ?: 1
            }
            // println("Bigrams of $word: $bigramsOfWord")
            return bigramsOfWord
        }

        private fun getFromIndex(word: String, index: Int): Pair<String, Int> {
            if(word[index] == endOfWordToken[0]) {
                val matchStart = notEndOfWordTokenRegex.find(word, index)
                val start = matchStart?.range?.first ?: index
                val matchEnd = endOfWordTokenRegex.find(word, index + 1)
                val end = matchEnd?.range?.first ?: word.length
                println("Bounds $word $index: $start, $end")
                return word.substring(start, end) to end
            } else {
                return word[index].toString() to index
            }
        
        }
    }


    
    fun train(corpus: String, merges: Int) {
        // 1. Initialize vocabulary
        initializeVocabulary(corpus)
        for (i in 0..merges)
            train()
    }

    fun train() {
        // 2. Count pairs of symbols
        val pairs: Map<String, Int> = countSymbolPairs(vocab)
        // 3. Find the most frequent pair & merge them
        val sortedByFrequency: List<Pair<String, Int>> = pairs.toList().sortedByDescending { it.second }
        merge(sortedByFrequency[0].first, vocab)
    }

    private fun merge(pair: String, vocab: Map<String, Int>) {
        val newVocab = mutableMapOf<String, Int>()
        val replacement = "$endOfWordToken$pair$endOfWordToken"
        println()
        println("Vocab: ${vocab}")
        println("Merging: $pair -> $replacement")

        vocab.forEach { (word, count) ->
            if (word.contains(pair)) {
                val newWord = word.replace(pair, replacement)
                println("Vocab updated: $word -> $newWord")
                newVocab[newWord] = count
            } else {
                println("Carried to new vocab: $word -> $word")
                newVocab[word] = count
            }
        }

        this.mergeTable[pair] = replacement
        this.vocab = newVocab
        println("New vocab: $newVocab")
        println()
    }

    private fun initializeVocabulary(corpus: String) {
        corpus.split(" ").forEach { entry ->
            this.vocab[entry] = this.vocab[entry]?.plus(1) ?: 1
        }
    }

    fun inferenceEncode(text: String): String {
        var encoded = text
        mergeTable.forEach { (pair, replacement) ->
            encoded = encoded.replace(pair, replacement)
        }
        return encoded
    }

    fun inferenceDecode(text: String): String {
        var decoded = text
        mergeTable.forEach { (pair, replacement) ->
            decoded = decoded.replace(replacement, pair)
        }
        return decoded
    }


}
