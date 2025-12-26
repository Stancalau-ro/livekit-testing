var DataChannelHelpers = (function() {
    function sendDataMessage(message, reliable, destinationIdentities) {
        if (!window.liveKitClient || !window.liveKitClient.room) {
            setLastError('No active room connection');
            setPublishingBlocked(true);
            return false;
        }
        var permissions = window.liveKitClient.room.localParticipant.permissions;
        if (permissions && permissions.canPublishData === false) {
            setLastError('Permission denied: canPublishData is false');
            setPublishingBlocked(true);
            console.warn('Data publishing blocked: canPublishData permission is false');
            return false;
        }
        try {
            var encoder = new TextEncoder();
            var data = encoder.encode(message);
            var options = { reliable: reliable !== false };
            if (destinationIdentities && Array.isArray(destinationIdentities)) {
                options.destinationIdentities = destinationIdentities;
            }
            window.liveKitClient.room.localParticipant.publishData(data, options)
                .then(function() {
                    addSentMessage({
                        content: message,
                        reliable: reliable !== false,
                        destinationIdentities: destinationIdentities || null,
                        timestamp: Date.now(),
                        size: data.length
                    });
                })
                .catch(function(e) {
                    console.error('Async error sending data message:', e);
                    setLastError(e.message || e.toString());
                    if (window.ErrorClassifier && window.ErrorClassifier.isPermissionError(e)) {
                        setPublishingBlocked(true);
                    }
                });
            return true;
        } catch (e) {
            console.error('Failed to send data message:', e);
            setLastError(e.message || e.toString());
            if (window.ErrorClassifier && window.ErrorClassifier.isPermissionError(e)) {
                setPublishingBlocked(true);
            }
            return false;
        }
    }

    function sendDataMessageOfSize(sizeBytes, reliable) {
        var message = 'X'.repeat(sizeBytes);
        var encoder = new TextEncoder();
        var actualSize = encoder.encode(message).length;
        if (actualSize !== sizeBytes) {
            console.warn('Message size mismatch: requested ' + sizeBytes + ', got ' + actualSize);
        }
        return sendDataMessage(message, reliable, null);
    }

    function sendTimestampedDataMessage(message, reliable) {
        var timestampedMessage = JSON.stringify({
            content: message,
            timestamp: Date.now()
        });
        return sendDataMessage(timestampedMessage, reliable, null);
    }

    function getReceivedMessages() {
        if (window.TestStateStore) return window.TestStateStore.dataChannel.getMessages();
        return window.dataChannelMessages || [];
    }

    function getReceivedMessageCount() {
        if (window.TestStateStore) return window.TestStateStore.dataChannel.getMessageCount();
        return window.dataChannelMessages ? window.dataChannelMessages.length : 0;
    }

    function findReceivedMessage(expectedContent, fromIdentity) {
        var messages = getReceivedMessages();
        for (var i = 0; i < messages.length; i++) {
            if (messages[i].content === expectedContent) {
                if (!fromIdentity || messages[i].from === fromIdentity) {
                    return messages[i];
                }
            }
        }
        return null;
    }

    function hasReceivedMessage(expectedContent, fromIdentity) {
        return findReceivedMessage(expectedContent, fromIdentity) !== null;
    }

    function getMessagesFromSender(senderIdentity) {
        var messages = getReceivedMessages();
        return messages.filter(function(msg) {
            return msg.from === senderIdentity;
        });
    }

    function isPublishingBlocked() {
        if (window.TestStateStore) return window.TestStateStore.dataChannel.isPublishingBlocked();
        return window.dataPublishingBlocked || false;
    }

    function setPublishingBlocked(blocked) {
        if (window.TestStateStore) window.TestStateStore.dataChannel.setPublishingBlocked(blocked);
        window.dataPublishingBlocked = blocked;
    }

    function getLastError() {
        if (window.TestStateStore) return window.TestStateStore.dataChannel.getLastError();
        return window.lastDataChannelError || '';
    }

    function setLastError(error) {
        if (window.TestStateStore) window.TestStateStore.dataChannel.setLastError(error);
        window.lastDataChannelError = error;
    }

    function addSentMessage(message) {
        if (window.TestStateStore) window.TestStateStore.dataChannel.addSentMessage(message);
        window.dataMessagesSent = window.dataMessagesSent || [];
        window.dataMessagesSent.push(message);
    }

    function getLatencyStats() {
        var messages = getReceivedMessages();
        var latencies = [];
        for (var i = 0; i < messages.length; i++) {
            if (messages[i].latency !== undefined) {
                latencies.push(messages[i].latency);
            }
        }
        if (latencies.length === 0) {
            return { count: 0, min: 0, max: 0, average: 0 };
        }
        var sum = latencies.reduce(function(a, b) { return a + b; }, 0);
        var min = Math.min.apply(null, latencies);
        var max = Math.max.apply(null, latencies);
        var average = sum / latencies.length;
        return { count: latencies.length, min: min, max: max, average: average };
    }

    function clearState() {
        if (window.TestStateStore) window.TestStateStore.dataChannel.clear();
        window.dataChannelMessages = [];
        window.dataMessagesSent = [];
        window.lastDataChannelError = '';
        window.dataPublishingBlocked = false;
    }

    function getSentMessageCount() {
        if (window.TestStateStore) return window.TestStateStore.dataChannel.getSentMessageCount();
        return window.dataMessagesSent ? window.dataMessagesSent.length : 0;
    }

    return {
        sendDataMessage: sendDataMessage,
        sendDataMessageOfSize: sendDataMessageOfSize,
        sendTimestampedDataMessage: sendTimestampedDataMessage,
        getReceivedMessages: getReceivedMessages,
        getReceivedMessageCount: getReceivedMessageCount,
        findReceivedMessage: findReceivedMessage,
        hasReceivedMessage: hasReceivedMessage,
        getMessagesFromSender: getMessagesFromSender,
        isPublishingBlocked: isPublishingBlocked,
        getLastError: getLastError,
        getLatencyStats: getLatencyStats,
        clearState: clearState,
        getSentMessageCount: getSentMessageCount
    };
})();

window.DataChannelHelpers = DataChannelHelpers;
