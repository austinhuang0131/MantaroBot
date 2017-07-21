package net.kodehawa.mantarobot.core.listeners.operations;

import br.com.brjdevs.java.utils.async.threads.builder.ThreadBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.utils.TimeAmount;

import javax.xml.ws.Holder;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("Duplicates")
public class ReactionOperation {
	private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(
		new ThreadBuilder().setName("ReactionOperations Executor")
	);

	private static final Map<String, ReactionOperation> OPERATIONS = new ConcurrentHashMap<>();
	private static final EventListener LISTENER = new EventListener() {
        @Override
        public void onEvent(Event e) {
            if (e instanceof MessageReactionAddEvent) {
                MessageReactionAddEvent event = (MessageReactionAddEvent) e;
                if (event.getReaction().isSelf()) return;

                String id = event.getMessageId();
                ReactionOperation operation = OPERATIONS.get(id);

                if (operation != null && operation.onReaction != null) {
                    if (operation.timeoutFuture != null) {
                        operation.timeoutFuture.cancel(true);
                        operation.timeoutFuture = null;
                    }

                    switch (operation.onReaction.apply(event)) {
                        case RESET_TIMEOUT:
                            scheduleTimeout(operation, false);
                            break;
                        case COMPLETED:
                            operation.onRemoved.run();
                            OPERATIONS.remove(id, operation);
                            break;
                    }
                }
            } else if (e instanceof MessageReactionRemoveEvent) {
                MessageReactionRemoveEvent event = (MessageReactionRemoveEvent) e;
                if (event.getReaction().isSelf()) return;

                String id = event.getMessageId();
                ReactionOperation operation = OPERATIONS.get(id);

                if (operation != null && operation.onReactionRemoved != null) {
                    if (operation.timeoutFuture != null) {
                        operation.timeoutFuture.cancel(true);
                        operation.timeoutFuture = null;
                    }

                    switch (operation.onReactionRemoved.apply(event)) {
                        case RESET_TIMEOUT:
                            scheduleTimeout(operation, false);
                            break;
                        case COMPLETED:
                            operation.onRemoved.run();
                            OPERATIONS.remove(id, operation);
                            break;
                    }
                }

            } else if (e instanceof MessageReactionRemoveAllEvent) {
                MessageReactionRemoveAllEvent event = (MessageReactionRemoveAllEvent) e;


                String id = event.getMessageId();
                ReactionOperation operation = OPERATIONS.get(id);

                if (operation != null && operation.onAllReactionsRemoved != null) {
                    if (operation.timeoutFuture != null) {
                        operation.timeoutFuture.cancel(true);
                        operation.timeoutFuture = null;
                    }

                    switch (operation.onAllReactionsRemoved.apply(event)) {
                        case RESET_TIMEOUT:
                            scheduleTimeout(operation, false);
                            break;
                        case COMPLETED:
                            operation.onRemoved.run();
                            OPERATIONS.remove(id, operation);
                            break;
                    }
                }
            }
        }
    };

	public static ReactionOperationBuilder builder() {
		return new ReactionOperationBuilder();
	}

	public static EventListener listener() {
		return LISTENER;
	}

	public static void stopOperation(String messageId) {
		ReactionOperation operation = OPERATIONS.remove(messageId);

		if (operation != null) {
			if (operation.timeoutFuture != null) {
				operation.timeoutFuture.cancel(true);
				operation.timeoutFuture = null;
			}

			if (operation.onRemoved != null) {
				operation.onRemoved.run();
			}
		}
	}

    private static void scheduleTimeout(ReactionOperation operation, boolean first) {
        TimeAmount timeAmount = first ? operation.initialTimeout : operation.increasingTimeout;

        if (timeAmount == null) return;

        operation.timeoutFuture = EXECUTOR.schedule(
            () -> {
                OPERATIONS.remove(operation.messageId, operation);
                if (operation.onTimeout != null) {
                    operation.onTimeout.run();
                }
            }, timeAmount.getAmount(), timeAmount.getUnit()
        );
    }

	private final String messageId;
    private final TimeAmount initialTimeout;
    private final TimeAmount increasingTimeout;
    private final Function<MessageReactionAddEvent,OperationResult> onReaction;
    private final Function<MessageReactionRemoveEvent, OperationResult> onReactionRemoved;
    private final Function<MessageReactionRemoveAllEvent, OperationResult> onAllReactionsRemoved;
    private final Runnable onTimeout;
    private final Runnable onRemoved;
    private Future<?> timeoutFuture;

	ReactionOperation(
	    Message message, Collection<String> reactions, TimeAmount initialTimeout, TimeAmount increasingTimeout,
        Function<MessageReactionAddEvent,OperationResult> onReaction,
        Function<MessageReactionRemoveEvent,OperationResult> onReactionRemoved,
        Function<MessageReactionRemoveAllEvent,OperationResult> onAllReactionsRemoved,
        Runnable onTimeout, Runnable onRemoved,
        boolean force
    ) {
		this.messageId = message.getId();
        this.initialTimeout = initialTimeout;
        this.increasingTimeout = increasingTimeout;
        this.onReaction = onReaction;
        this.onReactionRemoved = onReactionRemoved;
        this.onAllReactionsRemoved = onAllReactionsRemoved;
        this.onTimeout = onTimeout;
        this.onRemoved = onRemoved;

        if (!force && OPERATIONS.containsKey(messageId))
			throw new IllegalStateException("Operation already happening at messageId");

		OPERATIONS.put(messageId, this);

        scheduleTimeout(this, true);

		if (!reactions.isEmpty()) {
			Iterator<String> iterator = reactions.iterator();
			Holder<Consumer<Void>> chain = new Holder<>();
			chain.value = nil -> {
				if (iterator.hasNext()) {
					message.addReaction(iterator.next()).queue(chain.value);
				}
			};

			message.clearReactions().queue(chain.value);
		}
	}
}
