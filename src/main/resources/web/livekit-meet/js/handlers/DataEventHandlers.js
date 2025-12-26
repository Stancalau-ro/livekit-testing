var DataEventHandlers = (function() {
    function create(client) {
        function onDataReceived(payload, participant, kind, topic) {
            try {
                const decoder = new TextDecoder();
                const content = decoder.decode(payload);
                const receiveTimestamp = Date.now();
                const messageObj = {
                    content: content,
                    from: participant ? participant.identity : 'unknown',
                    kind: kind,
                    topic: topic,
                    timestamp: receiveTimestamp,
                    size: payload.length
                };

                try {
                    const parsed = JSON.parse(content);
                    if (parsed.timestamp) {
                        messageObj.sentTimestamp = parsed.timestamp;
                        messageObj.latency = receiveTimestamp - parsed.timestamp;
                        messageObj.content = parsed.content;
                    }
                } catch (e) {
                }

                if (window.TestStateStore) {
                    window.TestStateStore.dataChannel.addMessage(messageObj);
                    window.TestStateStore.syncToWindow();
                } else {
                    window.dataChannelMessages = window.dataChannelMessages || [];
                    window.dataChannelMessages.push(messageObj);
                }

                addTechnicalDetail('📨 Data received from ' + (participant ? participant.identity : 'unknown') +
                    ': ' + content.substring(0, 50) + (content.length > 50 ? '...' : ''));
            } catch (error) {
                console.error('Error processing data message:', error);
                addTechnicalDetail('❌ Data receive error: ' + error.message);
            }
        }

        function attach(room) {
            room.on(LiveKit.RoomEvent.DataReceived, onDataReceived);
        }

        return {
            onDataReceived: onDataReceived,
            attach: attach
        };
    }

    return {
        create: create
    };
})();

window.DataEventHandlers = DataEventHandlers;
