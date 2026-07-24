package dev.totem.automata.copper;

import java.util.List;

/** Cache-first target-rule policy used by the gathering scanner. */
public final class GatheringTargetPolicy {
    private GatheringTargetPolicy() { }

    public static boolean hasRules(List<String> manualTargets, GatheringLlmState.Config llm, GolemLlmState.Config golem) {
        return !manualTargets.isEmpty() || llm.usable(golem);
    }

    /**
     * Resolves only the manual/LLM rule portion of target eligibility.  The
     * caller still checks world safety, permissions, movement, and storage.
     */
    public static Decision decide(String blockId, List<String> blockTags, List<String> manualTargets,
            GatheringLlmState.Config llm, GolemLlmState.Config golem) {
        if (manualTargets.contains(blockId)) return Decision.ALLOW_MANUAL;
        if (!llm.usable(golem)) return Decision.DENY_NO_RULE;
        return GatheringLlmState.cachedDecision(llm, blockId, blockTags)
                .map(value -> value ? Decision.ALLOW_CACHE : Decision.DENY_CACHE)
                .orElse(Decision.REQUEST_CLASSIFICATION);
    }

    public enum Decision {
        ALLOW_MANUAL(true, false), ALLOW_CACHE(true, false), DENY_NO_RULE(false, false),
        DENY_CACHE(false, false), REQUEST_CLASSIFICATION(false, true);
        private final boolean allowed, requestsClassification;
        Decision(boolean allowed, boolean requestsClassification) { this.allowed = allowed; this.requestsClassification = requestsClassification; }
        public boolean allowed() { return allowed; }
        public boolean requestsClassification() { return requestsClassification; }
    }
}
