var ParticipantEventHandlers = (function() {
    function create(client) {
        function onParticipantConnected(participant) {
            console.log('*** ParticipantConnected EVENT FIRED ***', participant.identity);
            console.log('Participant details:', {
                identity: participant.identity,
                tracks: participant.trackPublications.size,
                trackList: Array.from(participant.trackPublications.values()).map(function(pub) {
                    return {
                        kind: pub.kind,
                        sid: pub.trackSid,
                        subscribed: pub.isSubscribed
                    };
                })
            });
            addTechnicalDetail(`👤 Participant connected: ${participant.identity}`);

            if (client && client.handleParticipantConnected) {
                client.handleParticipantConnected(participant);
            }

            setTimeout(function() {
                console.log('*** MANUAL SUBSCRIPTION ATTEMPT ***', participant.identity);
                participant.trackPublications.forEach(async function(publication, trackSid) {
                    console.log('Checking publication:', {
                        kind: publication.kind,
                        sid: trackSid,
                        subscribed: publication.isSubscribed,
                        track: !!publication.track
                    });

                    if (!publication.isSubscribed && publication.kind === 'video') {
                        console.log('Manually subscribing to video track:', trackSid);
                        addTechnicalDetail(`🔔 Manually subscribing to video track from ${participant.identity}`);
                        try {
                            await publication.setSubscribed(true);
                            console.log('Manual subscription successful for:', trackSid);
                        } catch (error) {
                            console.error('Manual subscription failed:', error);
                            addTechnicalDetail(`❌ Manual subscription failed: ${error.message}`);
                            if (client && client.handleSubscriptionError) {
                                client.handleSubscriptionError(error, trackSid, participant.identity, 'manual');
                            }
                        }
                    }
                });
            }, 1000);
        }

        function onParticipantDisconnected(participant) {
            console.log('Participant disconnected:', participant.identity);
            addTechnicalDetail(`👤 Participant disconnected: ${participant.identity}`);
            if (client && client.handleParticipantDisconnected) {
                client.handleParticipantDisconnected(participant);
            }
        }

        function attach(room) {
            room.on(LiveKit.RoomEvent.ParticipantConnected, onParticipantConnected);
            room.on(LiveKit.RoomEvent.ParticipantDisconnected, onParticipantDisconnected);
        }

        return {
            onParticipantConnected: onParticipantConnected,
            onParticipantDisconnected: onParticipantDisconnected,
            attach: attach
        };
    }

    return {
        create: create
    };
})();

window.ParticipantEventHandlers = ParticipantEventHandlers;
