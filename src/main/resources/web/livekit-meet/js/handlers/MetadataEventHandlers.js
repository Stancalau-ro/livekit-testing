var MetadataEventHandlers = (function() {
    function create(client) {
        function onRoomMetadataChanged(metadata) {
            console.log('*** RoomMetadataChanged EVENT ***', metadata);
            addTechnicalDetail(`📋 Room metadata changed: ${metadata ? metadata.substring(0, 50) : 'null'}`);
            const roomEvent = {
                metadata: metadata,
                timestamp: Date.now()
            };
            if (window.TestStateStore) {
                window.TestStateStore.metadata.addRoomEvent(roomEvent);
                window.TestStateStore.syncToWindow();
            } else {
                window.roomMetadataEvents = window.roomMetadataEvents || [];
                window.roomMetadataEvents.push(roomEvent);
            }
        }

        function onParticipantMetadataChanged(prevMetadata, participant) {
            const newMetadata = participant.metadata || '';
            console.log('*** ParticipantMetadataChanged EVENT ***', participant.identity, 'prev:', prevMetadata, 'new:', newMetadata);
            addTechnicalDetail(`📋 Participant ${participant.identity} metadata changed: prev='${prevMetadata}' new='${newMetadata ? newMetadata.substring(0, 50) : 'null'}'`);
            const participantEvent = {
                participantIdentity: participant.identity,
                metadata: newMetadata,
                prevMetadata: prevMetadata,
                timestamp: Date.now()
            };
            if (window.TestStateStore) {
                window.TestStateStore.metadata.addParticipantEvent(participantEvent);
                window.TestStateStore.syncToWindow();
            } else {
                window.participantMetadataEvents = window.participantMetadataEvents || [];
                window.participantMetadataEvents.push(participantEvent);
            }
        }

        function attach(room) {
            const roomMetadataEvent = LiveKit.RoomEvent?.RoomMetadataChanged ?? 'roomMetadataChanged';
            const participantMetadataEvent = LiveKit.RoomEvent?.ParticipantMetadataChanged ?? 'participantMetadataChanged';

            console.log('Registering RoomMetadataChanged event listener with event:', roomMetadataEvent);
            room.on(roomMetadataEvent, onRoomMetadataChanged);

            console.log('Registering ParticipantMetadataChanged event listener with event:', participantMetadataEvent);
            room.on(participantMetadataEvent, onParticipantMetadataChanged);
        }

        return {
            onRoomMetadataChanged: onRoomMetadataChanged,
            onParticipantMetadataChanged: onParticipantMetadataChanged,
            attach: attach
        };
    }

    return {
        create: create
    };
})();

window.MetadataEventHandlers = MetadataEventHandlers;
