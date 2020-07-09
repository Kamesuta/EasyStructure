package net.teamfruit.easystructure;

import org.bukkit.command.CommandSender;

public class ActionResult {
    private ResultType type;
    private String message;
    private String[] details;

    private ActionResult(final ResultType type, final String message, final String... details) {
        this.type = type;
        this.message = message;
        this.details = details;
    }

    public ResultType getType() {
        return this.type;
    }

    public String getMessage() {
        return this.message;
    }

    public String[] getDetails() {
        return this.details;
    }

    public static ActionResult success() {
        return new ActionResult(ResultType.SUCCESS, null);
    }

    public static ActionResult error() {
        return new ActionResult(ResultType.ERROR, null);
    }

    public static ActionResult error(final String message, final String... details) {
        return new ActionResult(ResultType.ERROR, message, details);
    }

    public enum ResultType {
        SUCCESS,
        ERROR,
    }

    public static boolean sendResultMessage(final ActionResult result, final CommandSender sender) {
        switch (result.getType()) {
            case ERROR: {
                final String message = result.getMessage();
                if (message != null)
                    sender.sendMessage("Error: " + message);
                final String[] details = result.getDetails();
                for (final String detail : details)
                    sender.sendMessage("  " + detail);
                return false;
            }
            default:
            case SUCCESS:
                return true;
        }
    }
}