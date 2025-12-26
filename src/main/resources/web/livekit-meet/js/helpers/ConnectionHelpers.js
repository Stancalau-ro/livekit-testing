var ConnectionHelpers = (function() {
    function isLiveKitLoaded() {
        return typeof LiveKit !== 'undefined';
    }

    function getLastError() {
        return window.TestStateStore
            ? window.TestStateStore.errors.getLast()
            : (window.lastError || '');
    }

    function isUsingMock() {
        return !!window.USING_MOCK_LIVEKIT;
    }

    function isRealWebRTCConnectionVerified() {
        return window.TestStateStore
            ? window.TestStateStore.connection.isRealWebRTCVerified()
            : (window.REAL_WEBRTC_CONNECTION_VERIFIED || false);
    }

    function isConnectionEstablished() {
        return window.TestStateStore
            ? window.TestStateStore.connection.isEstablished()
            : (window.connectionEstablished || false);
    }

    function isClientConnected() {
        return window.liveKitClient && window.liveKitClient.isConnected();
    }

    function getConnectionTime() {
        return window.TestStateStore
            ? window.TestStateStore.connection.getTime()
            : (window.connectionTime || 0);
    }

    function getConsoleLogs() {
        if (window.TestStateStore) {
            var logs = window.TestStateStore.console.getLogsAsString();
            return logs || 'No console logs captured';
        }
        return window.consoleLogCapture ? window.consoleLogCapture.join('\n') : 'No console logs captured';
    }

    function isInMeetingRoom() {
        return window.liveKitClient && window.liveKitClient.isInMeetingRoom();
    }

    return {
        isLiveKitLoaded: isLiveKitLoaded,
        getLastError: getLastError,
        isUsingMock: isUsingMock,
        isRealWebRTCConnectionVerified: isRealWebRTCConnectionVerified,
        isConnectionEstablished: isConnectionEstablished,
        isClientConnected: isClientConnected,
        getConnectionTime: getConnectionTime,
        getConsoleLogs: getConsoleLogs,
        isInMeetingRoom: isInMeetingRoom
    };
})();

window.ConnectionHelpers = ConnectionHelpers;
