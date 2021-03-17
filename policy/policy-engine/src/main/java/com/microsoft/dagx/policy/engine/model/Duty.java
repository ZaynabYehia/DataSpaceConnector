package com.microsoft.dagx.policy.engine.model;

import org.jetbrains.annotations.Nullable;

import static java.util.stream.Collectors.joining;

/**
 * An obligation that must be performed if all its constraints are satisfied.
 */
public class Duty extends Rule {
    @Nullable
    private Duty consequence;

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitDuty(this);
    }

    @Override
    public String toString() {
        return "Duty constraint: [" + getConstraints().stream().map(Object::toString).collect(joining(",")) + "]";
    }

    public static class Builder extends Rule.Builder<Duty, Duty.Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Duty build() {
            return rule;
        }

        private Builder() {
            rule = new Duty();
        }
    }

}
