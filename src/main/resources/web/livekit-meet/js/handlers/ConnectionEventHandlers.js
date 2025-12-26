var ConnectionEventHandlers = (function() {
    function create(client) {
        function onConnected() {
            const connectionEndTime = Date.now();
            const connectionTime = connectionEndTime - (window.connectionStartTime || connectionEndTime);

            if (window.TestStateStore) {
                window.TestStateStore.connection.setTime(connectionTime);
                window.TestStateStore.syncToWindow();
            } else {
                window.connectionTime = connectionTime;
            }

            console.log('*** Connected event fired ***');
            console.log('Connection time:', connectionTime, 'ms');
            addTechnicalDetail(`📡 Connected event fired after ${connectionTime}ms`);
        }

        function onDisconnected() {
            console.log('*** DISCONNECTED from room ***');
            addTechnicalDetail('⚠️ Disconnected from room');
            if (client && client.handleDisconnection) {
                client.handleDisconnection();
            }
        }

        function onReconnecting() {
            console.log('*** RECONNECTING to room ***');
            addTechnicalDetail('🔄 Reconnecting to room...');
            updateStatus('Reconnecting...', 'info');
        }

        function onReconnected() {
            console.log('*** RECONNECTED to room ***');
            addTechnicalDetail('✅ Reconnected to room');
            updateStatus('Reconnected', 'success');
        }

        function onError(error) {
            console.error('*** ROOM ERROR ***', error);
            addTechnicalDetail(`❌ Room error: ${error.message || error}`);
            markStepFailed('step-webrtc', `Error: ${error.message || error}`);
        }

        function onConnectionQualityChanged(quality, participant) {
            console.log('Connection quality changed:', quality, participant?.identity);
            addTechnicalDetail(`Connection quality: ${quality} for ${participant?.identity || 'local'}`);
        }

        function onMediaDevicesError(error) {
            console.error('*** MEDIA DEVICES ERROR ***', error);
            addTechnicalDetail(`❌ Media devices error: ${error.message || error}`);
        }

        function attach(room) {
            const connectedEvent = LiveKit.RoomEvent ? LiveKit.RoomEvent.Connected : 'connected';
            const disconnectedEvent = LiveKit.RoomEvent ? LiveKit.RoomEvent.Disconnected : 'disconnected';

            room.on(connectedEvent, onConnected);
            room.on(disconnectedEvent, onDisconnected);
            room.on(LiveKit.RoomEvent.ConnectionQualityChanged, onConnectionQualityChanged);
            room.on(LiveKit.RoomEvent.Reconnecting, onReconnecting);
            room.on(LiveKit.RoomEvent.Reconnected, onReconnected);
            room.on('error', onError);
            room.on('mediaDevicesError', onMediaDevicesError);

            if (room.engine) {
                room.engine.on('connectionStateChanged', function(state) {
                    console.log('*** ENGINE CONNECTION STATE CHANGED ***', state);
                    addTechnicalDetail(`Engine connection state: ${state}`);
                });
                room.engine.on('signalingStateChanged', function(state) {
                    console.log('*** ENGINE SIGNALING STATE CHANGED ***', state);
                    addTechnicalDetail(`Signaling state: ${state}`);
                });
            }
        }

        return {
            onConnected: onConnected,
            onDisconnected: onDisconnected,
            onReconnecting: onReconnecting,
            onReconnected: onReconnected,
            onError: onError,
            onConnectionQualityChanged: onConnectionQualityChanged,
            onMediaDevicesError: onMediaDevicesError,
            attach: attach
        };
    }

    return {
        create: create
    };
})();

window.ConnectionEventHandlers = ConnectionEventHandlers;
