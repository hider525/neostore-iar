package com.neostudio.iar;

public abstract class ReviewResult {
    private ReviewResult() {
    }

    public static final class Submitted extends ReviewResult {
        private final int rating;
        private final String comment;

        public Submitted(int rating, String comment) {
            this.rating = rating;
            this.comment = comment;
        }

        public int getRating() {
            return rating;
        }

        public String getComment() {
            return comment;
        }
    }

    public static final class Cancelled extends ReviewResult {
    }

    public static final class AlreadyReviewed extends ReviewResult {
    }

    public static final class NotAuthenticated extends ReviewResult {
    }

    public static final class Error extends ReviewResult {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
