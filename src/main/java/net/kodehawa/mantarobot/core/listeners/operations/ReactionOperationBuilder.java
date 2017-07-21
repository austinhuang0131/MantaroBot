package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.kodehawa.mantarobot.utils.TimeAmount;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReactionOperationBuilder {
    private final List<String> reactions = new LinkedList<>();
    private Message message;
    private Function<MessageReactionAddEvent,OperationResult> onReaction;
    private Function<MessageReactionRemoveEvent,OperationResult> onReactionRemoved;
    private Function<MessageReactionRemoveAllEvent,OperationResult> onAllReactionsRemoved;
    private Runnable onRemoved;
    private Runnable onTimeout;
    private TimeAmount increasingTimeout;
    private TimeAmount initialTimeout;

    ReactionOperationBuilder() {
    }

    public ReactionOperationBuilder addReactions(String... reactions) {
        Collections.addAll(this.reactions, reactions);
        return this;
    }

    public ReactionOperationBuilder addReactions(EmoteReference... reactions) {
        for (EmoteReference reaction : reactions) this.reactions.add(reaction.getUnicode());
        return this;
    }

    public void create(Runnable success, Consumer<Throwable> failure) {
        try {
            new ReactionOperation(
                Objects.requireNonNull(message, "message"),
                reactions,
                Objects.requireNonNull(initialTimeout, "initialTimeout"),
                increasingTimeout,
                onReaction,
                onReactionRemoved,
                onAllReactionsRemoved,
                onTimeout,
                onRemoved,
                false
            );

            success.run();
        } catch (Exception e) {
            failure.accept(e);
        }
    }

    public void create(Runnable success) {
        create(success, e -> {});
    }

    public void create() {
        create(() -> {});
    }

    public void forceCreate() {
        new ReactionOperation(
            Objects.requireNonNull(message, "message"),
            reactions,
            Objects.requireNonNull(initialTimeout, "initialTimeout"),
            increasingTimeout,
            onReaction,
            onReactionRemoved,
            onAllReactionsRemoved,
            onTimeout,
            onRemoved,
            true
        );
    }

    public ReactionOperationBuilder increasingTimeout(long amount, TimeUnit unit) {
        this.increasingTimeout = new TimeAmount(amount, unit);
        return this;
    }

    public ReactionOperationBuilder initialTimeout(long amount, TimeUnit unit) {
        this.initialTimeout = new TimeAmount(amount, unit);
        return this;
    }

    public ReactionOperationBuilder message(Message message) {
        this.message = message;
        return this;
    }

    public ReactionOperationBuilder onReaction(Function<MessageReactionAddEvent,OperationResult> onReaction) {
        this.onReaction = onReaction;
        return this;
    }

    public ReactionOperationBuilder onReactionRemoved(Function<MessageReactionRemoveEvent,OperationResult> onReactionRemoved) {
        this.onReactionRemoved = onReactionRemoved;
        return this;
    }

    public ReactionOperationBuilder onAllReactionsRemoved(Function<MessageReactionRemoveAllEvent,OperationResult> onAllReactionsRemoved) {
        this.onAllReactionsRemoved = onAllReactionsRemoved;
        return this;
    }

    public ReactionOperationBuilder onRemoved(Runnable onRemoved) {
        this.onRemoved = onRemoved;
        return this;
    }

    public ReactionOperationBuilder onTimeout(Runnable onTimeout) {
        this.onTimeout = onTimeout;
        return this;
    }

    public ReactionOperationBuilder timeout(long amount, TimeUnit unit) {
        this.initialTimeout = new TimeAmount(amount, unit);
        this.increasingTimeout = new TimeAmount(amount, unit);
        return this;
    }
}
