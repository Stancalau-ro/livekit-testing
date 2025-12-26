var ErrorClassifier = (function() {
    var PERMISSION_KEYWORDS = ['permission', 'denied', 'forbidden', 'unauthorized', 'not allowed'];
    var TIMEOUT_KEYWORDS = ['timeout', 'timed out', 'time out'];
    var NETWORK_KEYWORDS = ['network', 'connection', 'unreachable', 'offline'];
    var MEDIA_KEYWORDS = ['media', 'device', 'camera', 'microphone', 'audio', 'video'];

    function normalizeError(error) {
        if (!error) return '';
        if (typeof error === 'string') return error.toLowerCase();
        if (error.message) return error.message.toLowerCase();
        return error.toString().toLowerCase();
    }

    function containsKeyword(message, keywords) {
        for (var i = 0; i < keywords.length; i++) {
            if (message.includes(keywords[i])) {
                return true;
            }
        }
        return false;
    }

    function isPermissionError(error) {
        var msg = normalizeError(error);
        return containsKeyword(msg, PERMISSION_KEYWORDS);
    }

    function isTimeoutError(error) {
        var msg = normalizeError(error);
        return containsKeyword(msg, TIMEOUT_KEYWORDS);
    }

    function isNetworkError(error) {
        var msg = normalizeError(error);
        return containsKeyword(msg, NETWORK_KEYWORDS);
    }

    function isMediaError(error) {
        var msg = normalizeError(error);
        return containsKeyword(msg, MEDIA_KEYWORDS);
    }

    function classify(error) {
        if (isPermissionError(error)) return 'permission';
        if (isTimeoutError(error)) return 'timeout';
        if (isNetworkError(error)) return 'network';
        if (isMediaError(error)) return 'media';
        return 'unknown';
    }

    function getErrorMessage(error) {
        if (!error) return '';
        if (typeof error === 'string') return error;
        if (error.message) return error.message;
        return error.toString();
    }

    return {
        isPermissionError: isPermissionError,
        isTimeoutError: isTimeoutError,
        isNetworkError: isNetworkError,
        isMediaError: isMediaError,
        classify: classify,
        getErrorMessage: getErrorMessage
    };
})();

window.ErrorClassifier = ErrorClassifier;
