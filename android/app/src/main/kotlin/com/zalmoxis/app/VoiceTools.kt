package com.zalmoxis.app

import android.content.Context
import kotlinx.coroutines.CancellationException

/** Only explicit user utterances can invoke these existing scoped capabilities. */
internal object VoiceTools {
    suspend fun execute(context: Context, utterance: String): String? {
        val text = utterance.trim()
        return try {
            if (text.startsWith("remember that ", ignoreCase = true)) {
                val fact = text.substring(14).trim()
                LocalMemoryCore(context).use { it.saveUserFact(MemoryScopePolicy.PERSONAL_USER, MemoryScopePolicy.PERSONAL_CLIENT, fact) }
                "I saved that to your local memory."
            } else if (text.equals("what do you remember about me", true) || text.equals("what do you remember about me?", true)) {
                val facts = LocalMemoryCore(context).use { it.listFacts(MemoryScopePolicy.PERSONAL_USER, MemoryScopePolicy.PERSONAL_CLIENT, 10) }
                if (facts.isEmpty()) "There are no saved facts yet." else facts.joinToString(". ") { it.text }
            } else if (text.startsWith("find github repositories for ", true)) {
                val hits = NativeResearch.searchRepositories(text.substring(29).trim())
                if (hits.isEmpty()) "I found no matching repositories." else hits.take(3).joinToString(". ") { it.title + ": " + it.description }
            } else if (text.equals("check my agent bridge",true)) {
                val connection=AgentBridgeClient.load(context)
                if(connection==null) "No agent bridge is paired." else if(AgentBridgeClient.status(connection)) "Your agent bridge is connected." else "Your agent bridge is unavailable."
            } else if(text.startsWith("send to my agent bridge ",true)) {
                val connection=AgentBridgeClient.load(context)
                if(connection==null) "No agent bridge is paired." else {
                    AgentBridgeClient.send(connection,text.substring(24).trim()); "I sent your request to the paired agent bridge."
                }
            } else null
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { "I couldn't complete that request." }
    }
}
