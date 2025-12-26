var TestStateStore = (function() {
    var state = {
        subscription: {
            failedEvents: [],
            permissionDenied: false,
            lastError: ''
        },
        screenShare: {
            active: false,
            permissionDenied: false,
            lastError: ''
        },
        simulcast: {
            enabled: true,
            currentQuality: 'HIGH',
            receivingLayers: new Map()
        },
        mute: {
            events: []
        },
        dataChannel: {
            messages: [],
            messagesSent: [],
            lastError: '',
            publishingBlocked: false
        },
        metadata: {
            roomEvents: [],
            participantEvents: [],
            roomListening: false,
            participantListening: false
        },
        trackStream: {
            stateEvents: []
        },
        connection: {
            established: false,
            startTime: null,
            time: 0,
            realWebRTCVerified: false
        },
        console: {
            logs: []
        },
        errors: {
            last: ''
        },
        bitrate: {
            baselineCapture: null
        }
    };

    function getState() {
        return state;
    }

    function reset() {
        state.subscription.failedEvents = [];
        state.subscription.permissionDenied = false;
        state.subscription.lastError = '';
        state.screenShare.active = false;
        state.screenShare.permissionDenied = false;
        state.screenShare.lastError = '';
        state.simulcast.currentQuality = 'HIGH';
        state.simulcast.receivingLayers = new Map();
        state.mute.events = [];
        state.dataChannel.messages = [];
        state.dataChannel.messagesSent = [];
        state.dataChannel.lastError = '';
        state.dataChannel.publishingBlocked = false;
        state.metadata.roomEvents = [];
        state.metadata.participantEvents = [];
        state.metadata.roomListening = false;
        state.metadata.participantListening = false;
        state.trackStream.stateEvents = [];
        state.connection.established = false;
        state.connection.time = 0;
        state.connection.realWebRTCVerified = false;
        state.bitrate.baselineCapture = null;
    }

    var subscription = {
        addFailure: function(event) {
            state.subscription.failedEvents.push(event);
        },
        getFailures: function() {
            return state.subscription.failedEvents.slice();
        },
        getFailureCount: function() {
            return state.subscription.failedEvents.length;
        },
        setPermissionDenied: function(denied) {
            state.subscription.permissionDenied = denied;
        },
        isPermissionDenied: function() {
            return state.subscription.permissionDenied;
        },
        setLastError: function(error) {
            state.subscription.lastError = error;
        },
        getLastError: function() {
            return state.subscription.lastError;
        },
        clear: function() {
            state.subscription.failedEvents = [];
            state.subscription.permissionDenied = false;
            state.subscription.lastError = '';
        }
    };

    var screenShare = {
        setActive: function(active) {
            state.screenShare.active = active;
        },
        isActive: function() {
            return state.screenShare.active;
        },
        setPermissionDenied: function(denied) {
            state.screenShare.permissionDenied = denied;
        },
        isPermissionDenied: function() {
            return state.screenShare.permissionDenied;
        },
        setLastError: function(error) {
            state.screenShare.lastError = error;
        },
        getLastError: function() {
            return state.screenShare.lastError;
        }
    };

    var simulcast = {
        setEnabled: function(enabled) {
            state.simulcast.enabled = enabled;
        },
        isEnabled: function() {
            return state.simulcast.enabled;
        },
        setCurrentQuality: function(quality) {
            state.simulcast.currentQuality = quality;
        },
        getCurrentQuality: function() {
            return state.simulcast.currentQuality;
        },
        setReceivingLayer: function(identity, layer) {
            state.simulcast.receivingLayers.set(identity, layer);
        },
        getReceivingLayer: function(identity) {
            return state.simulcast.receivingLayers.get(identity) || null;
        }
    };

    var mute = {
        addEvent: function(event) {
            state.mute.events.push(event);
        },
        getEvents: function() {
            return state.mute.events.slice();
        },
        getEventCount: function() {
            return state.mute.events.length;
        },
        getLastEvent: function() {
            return state.mute.events.length > 0
                ? state.mute.events[state.mute.events.length - 1]
                : null;
        },
        clear: function() {
            state.mute.events = [];
        }
    };

    var dataChannel = {
        addMessage: function(message) {
            state.dataChannel.messages.push(message);
        },
        getMessages: function() {
            return state.dataChannel.messages.slice();
        },
        getMessageCount: function() {
            return state.dataChannel.messages.length;
        },
        addSentMessage: function(message) {
            state.dataChannel.messagesSent.push(message);
        },
        getSentMessages: function() {
            return state.dataChannel.messagesSent.slice();
        },
        getSentMessageCount: function() {
            return state.dataChannel.messagesSent.length;
        },
        setLastError: function(error) {
            state.dataChannel.lastError = error;
        },
        getLastError: function() {
            return state.dataChannel.lastError;
        },
        setPublishingBlocked: function(blocked) {
            state.dataChannel.publishingBlocked = blocked;
        },
        isPublishingBlocked: function() {
            return state.dataChannel.publishingBlocked;
        },
        clear: function() {
            state.dataChannel.messages = [];
            state.dataChannel.messagesSent = [];
            state.dataChannel.lastError = '';
            state.dataChannel.publishingBlocked = false;
        }
    };

    var metadata = {
        addRoomEvent: function(event) {
            state.metadata.roomEvents.push(event);
        },
        getRoomEvents: function() {
            return state.metadata.roomEvents.slice();
        },
        getRoomEventCount: function() {
            return state.metadata.roomEvents.length;
        },
        addParticipantEvent: function(event) {
            state.metadata.participantEvents.push(event);
        },
        getParticipantEvents: function() {
            return state.metadata.participantEvents.slice();
        },
        getParticipantEventCount: function() {
            return state.metadata.participantEvents.length;
        },
        setRoomListening: function(listening) {
            state.metadata.roomListening = listening;
        },
        isRoomListening: function() {
            return state.metadata.roomListening;
        },
        setParticipantListening: function(listening) {
            state.metadata.participantListening = listening;
        },
        isParticipantListening: function() {
            return state.metadata.participantListening;
        },
        clear: function() {
            state.metadata.roomEvents = [];
            state.metadata.participantEvents = [];
        }
    };

    var trackStream = {
        addStateEvent: function(event) {
            state.trackStream.stateEvents.push(event);
        },
        getStateEvents: function() {
            return state.trackStream.stateEvents.slice();
        },
        clear: function() {
            state.trackStream.stateEvents = [];
        }
    };

    var connection = {
        setEstablished: function(established) {
            state.connection.established = established;
        },
        isEstablished: function() {
            return state.connection.established;
        },
        setStartTime: function(time) {
            state.connection.startTime = time;
        },
        getStartTime: function() {
            return state.connection.startTime;
        },
        setTime: function(time) {
            state.connection.time = time;
        },
        getTime: function() {
            return state.connection.time;
        },
        setRealWebRTCVerified: function(verified) {
            state.connection.realWebRTCVerified = verified;
        },
        isRealWebRTCVerified: function() {
            return state.connection.realWebRTCVerified;
        }
    };

    var console = {
        addLog: function(log) {
            state.console.logs.push(log);
        },
        getLogs: function() {
            return state.console.logs.slice();
        },
        getLogsAsString: function() {
            return state.console.logs.join('\n');
        }
    };

    var errors = {
        setLast: function(error) {
            state.errors.last = error;
        },
        getLast: function() {
            return state.errors.last;
        }
    };

    var bitrate = {
        setBaselineCapture: function(capture) {
            state.bitrate.baselineCapture = capture;
        },
        getBaselineCapture: function() {
            return state.bitrate.baselineCapture;
        }
    };

    function syncToWindow() {
        window.subscriptionFailedEvents = state.subscription.failedEvents;
        window.subscriptionPermissionDenied = state.subscription.permissionDenied;
        window.lastSubscriptionError = state.subscription.lastError;
        window.screenSharePermissionDenied = state.screenShare.permissionDenied;
        window.screenShareActive = state.screenShare.active;
        window.lastScreenShareError = state.screenShare.lastError;
        window.simulcastEnabled = state.simulcast.enabled;
        window.currentVideoQuality = state.simulcast.currentQuality;
        window.receivingLayers = state.simulcast.receivingLayers;
        window.muteEvents = state.mute.events;
        window.dataChannelMessages = state.dataChannel.messages;
        window.dataMessagesSent = state.dataChannel.messagesSent;
        window.lastDataChannelError = state.dataChannel.lastError;
        window.dataPublishingBlocked = state.dataChannel.publishingBlocked;
        window.roomMetadataEvents = state.metadata.roomEvents;
        window.participantMetadataEvents = state.metadata.participantEvents;
        window.roomMetadataListening = state.metadata.roomListening;
        window.participantMetadataListening = state.metadata.participantListening;
        window.trackStreamStateEvents = state.trackStream.stateEvents;
        window.connectionEstablished = state.connection.established;
        window.connectionStartTime = state.connection.startTime;
        window.connectionTime = state.connection.time;
        window.REAL_WEBRTC_CONNECTION_VERIFIED = state.connection.realWebRTCVerified;
        window.consoleLogCapture = state.console.logs;
        window.lastError = state.errors.last;
        window.baselineVideoBytesCapture = state.bitrate.baselineCapture;
    }

    function initFromWindow() {
        if (window.subscriptionFailedEvents) state.subscription.failedEvents = window.subscriptionFailedEvents;
        if (window.subscriptionPermissionDenied !== undefined) state.subscription.permissionDenied = window.subscriptionPermissionDenied;
        if (window.lastSubscriptionError) state.subscription.lastError = window.lastSubscriptionError;
        if (window.screenSharePermissionDenied !== undefined) state.screenShare.permissionDenied = window.screenSharePermissionDenied;
        if (window.screenShareActive !== undefined) state.screenShare.active = window.screenShareActive;
        if (window.lastScreenShareError) state.screenShare.lastError = window.lastScreenShareError;
        if (window.simulcastEnabled !== undefined) state.simulcast.enabled = window.simulcastEnabled;
        if (window.currentVideoQuality) state.simulcast.currentQuality = window.currentVideoQuality;
        if (window.receivingLayers) state.simulcast.receivingLayers = window.receivingLayers;
        if (window.muteEvents) state.mute.events = window.muteEvents;
        if (window.dataChannelMessages) state.dataChannel.messages = window.dataChannelMessages;
        if (window.dataMessagesSent) state.dataChannel.messagesSent = window.dataMessagesSent;
        if (window.lastDataChannelError) state.dataChannel.lastError = window.lastDataChannelError;
        if (window.dataPublishingBlocked !== undefined) state.dataChannel.publishingBlocked = window.dataPublishingBlocked;
        if (window.roomMetadataEvents) state.metadata.roomEvents = window.roomMetadataEvents;
        if (window.participantMetadataEvents) state.metadata.participantEvents = window.participantMetadataEvents;
        if (window.roomMetadataListening !== undefined) state.metadata.roomListening = window.roomMetadataListening;
        if (window.participantMetadataListening !== undefined) state.metadata.participantListening = window.participantMetadataListening;
        if (window.trackStreamStateEvents) state.trackStream.stateEvents = window.trackStreamStateEvents;
        if (window.connectionEstablished !== undefined) state.connection.established = window.connectionEstablished;
        if (window.connectionStartTime) state.connection.startTime = window.connectionStartTime;
        if (window.connectionTime) state.connection.time = window.connectionTime;
        if (window.REAL_WEBRTC_CONNECTION_VERIFIED !== undefined) state.connection.realWebRTCVerified = window.REAL_WEBRTC_CONNECTION_VERIFIED;
        if (window.consoleLogCapture) state.console.logs = window.consoleLogCapture;
        if (window.lastError) state.errors.last = window.lastError;
        if (window.baselineVideoBytesCapture) state.bitrate.baselineCapture = window.baselineVideoBytesCapture;
    }

    return {
        getState: getState,
        reset: reset,
        subscription: subscription,
        screenShare: screenShare,
        simulcast: simulcast,
        mute: mute,
        dataChannel: dataChannel,
        metadata: metadata,
        trackStream: trackStream,
        connection: connection,
        console: console,
        errors: errors,
        bitrate: bitrate,
        syncToWindow: syncToWindow,
        initFromWindow: initFromWindow
    };
})();

window.TestStateStore = TestStateStore;
