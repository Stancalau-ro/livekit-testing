var MetadataHelpers = (function() {
    function startListeningForRoomMetadataEvents() {
        if (window.TestStateStore) window.TestStateStore.metadata.setRoomListening(true);
        window.roomMetadataListening = true;
    }

    function startListeningForParticipantMetadataEvents() {
        if (window.TestStateStore) window.TestStateStore.metadata.setParticipantListening(true);
        window.participantMetadataListening = true;
    }

    function getRoomMetadataEvents() {
        if (window.TestStateStore) return window.TestStateStore.metadata.getRoomEvents();
        return window.roomMetadataEvents || [];
    }

    function getParticipantMetadataEvents() {
        if (window.TestStateStore) return window.TestStateStore.metadata.getParticipantEvents();
        return window.participantMetadataEvents || [];
    }

    function getRoomMetadataEventCount() {
        if (window.TestStateStore) return window.TestStateStore.metadata.getRoomEventCount();
        return window.roomMetadataEvents ? window.roomMetadataEvents.length : 0;
    }

    function getParticipantMetadataEventCount() {
        if (window.TestStateStore) return window.TestStateStore.metadata.getParticipantEventCount();
        return window.participantMetadataEvents ? window.participantMetadataEvents.length : 0;
    }

    function getCurrentRoomMetadata() {
        if (!window.liveKitClient || !window.liveKitClient.room) return '';
        return window.liveKitClient.room.metadata || '';
    }

    function getParticipantMetadata(identity) {
        if (!window.liveKitClient || !window.liveKitClient.room) return '';
        var participant = null;
        window.liveKitClient.room.remoteParticipants.forEach(function(p) {
            if (p.identity === identity) participant = p;
        });
        if (!participant) {
            if (window.liveKitClient.room.localParticipant &&
                window.liveKitClient.room.localParticipant.identity === identity) {
                participant = window.liveKitClient.room.localParticipant;
            }
        }
        return participant ? (participant.metadata || '') : '';
    }

    function getLocalParticipantMetadata() {
        if (!window.liveKitClient || !window.liveKitClient.room) return '';
        var localParticipant = window.liveKitClient.room.localParticipant;
        return localParticipant ? (localParticipant.metadata || '') : '';
    }

    function waitForRoomMetadataEvent(expectedValue, timeoutSeconds) {
        var events = getRoomMetadataEvents();
        for (var i = 0; i < events.length; i++) {
            if (events[i].metadata === expectedValue) return true;
        }
        return false;
    }

    function waitForParticipantMetadataEvent(identity, expectedValue, timeoutSeconds) {
        var events = getParticipantMetadataEvents();
        for (var i = 0; i < events.length; i++) {
            if (events[i].participantIdentity === identity && events[i].metadata === expectedValue) {
                return true;
            }
        }
        return false;
    }

    function hasRoomMetadataEventWithValue(expectedValue) {
        var events = getRoomMetadataEvents();
        for (var i = 0; i < events.length; i++) {
            if (events[i].metadata === expectedValue) return true;
        }
        return false;
    }

    function hasParticipantMetadataEventFor(identity, expectedValue) {
        var events = getParticipantMetadataEvents();
        for (var i = 0; i < events.length; i++) {
            if (events[i].participantIdentity === identity && events[i].metadata === expectedValue) {
                return true;
            }
        }
        return false;
    }

    function clearMetadataEvents() {
        if (window.TestStateStore) window.TestStateStore.metadata.clear();
        window.roomMetadataEvents = [];
        window.participantMetadataEvents = [];
    }

    return {
        startListeningForRoomMetadataEvents: startListeningForRoomMetadataEvents,
        startListeningForParticipantMetadataEvents: startListeningForParticipantMetadataEvents,
        getRoomMetadataEvents: getRoomMetadataEvents,
        getParticipantMetadataEvents: getParticipantMetadataEvents,
        getRoomMetadataEventCount: getRoomMetadataEventCount,
        getParticipantMetadataEventCount: getParticipantMetadataEventCount,
        getCurrentRoomMetadata: getCurrentRoomMetadata,
        getParticipantMetadata: getParticipantMetadata,
        getLocalParticipantMetadata: getLocalParticipantMetadata,
        waitForRoomMetadataEvent: waitForRoomMetadataEvent,
        waitForParticipantMetadataEvent: waitForParticipantMetadataEvent,
        hasRoomMetadataEventWithValue: hasRoomMetadataEventWithValue,
        hasParticipantMetadataEventFor: hasParticipantMetadataEventFor,
        clearMetadataEvents: clearMetadataEvents
    };
})();

window.MetadataHelpers = MetadataHelpers;
