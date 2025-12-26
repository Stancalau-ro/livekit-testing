var TrackEventHandlers = (function() {
    function create(client) {
        function onTrackSubscribed(track, publication, participant) {
            console.log('*** TrackSubscribed EVENT FIRED ***', track.kind, participant.identity);
            console.log('Track details:', {
                kind: track.kind,
                sid: track.sid,
                participant: participant.identity,
                muted: track.isMuted,
                enabled: track.isEnabled
            });
            addTechnicalDetail(`🎥 Track subscribed: ${track.kind} from ${participant.identity}`);
            if (client && client.handleTrackSubscribed) {
                client.handleTrackSubscribed(track, participant);
            }
        }

        function onTrackUnsubscribed(track, publication, participant) {
            console.log('Track unsubscribed:', track.kind, participant.identity);
            addTechnicalDetail(`🎥 Track unsubscribed: ${track.kind} from ${participant.identity}`);
            if (client && client.handleTrackUnsubscribed) {
                client.handleTrackUnsubscribed(track, participant);
            }
        }

        function onTrackPublished(publication, participant) {
            console.log('*** TrackPublished EVENT ***', publication.kind, 'from', participant.identity);
            addTechnicalDetail(`📤 Track published: ${publication.kind} from ${participant.identity}`);

            if (client && client.room && publication.kind === 'video' &&
                participant.identity !== client.room.localParticipant.identity) {
                console.log('Forcing immediate subscription to video track:', publication.trackSid);
                addTechnicalDetail(`🔔 Forcing subscription to video track from ${participant.identity}`);
                setTimeout(async function() {
                    try {
                        await publication.setSubscribed(true);
                        console.log('Forced subscription successful:', publication.trackSid);
                        addTechnicalDetail(`✅ Forced subscription successful for ${participant.identity}`);
                    } catch (error) {
                        console.error('Forced subscription failed:', error);
                        addTechnicalDetail(`❌ Forced subscription failed: ${error.message}`);
                        if (client && client.handleSubscriptionError) {
                            client.handleSubscriptionError(error, publication.trackSid, participant.identity, 'forced');
                        }
                    }
                }, 100);
            }
        }

        function onTrackUnpublished(publication, participant) {
            console.log('*** TrackUnpublished EVENT ***', publication.kind, 'from', participant.identity);
            addTechnicalDetail(`📤 Track unpublished: ${publication.kind} from ${participant.identity}`);
        }

        function onTrackSubscriptionFailed(trackSid, participant, error) {
            console.error('*** TrackSubscriptionFailed EVENT ***', trackSid, participant.identity, error);
            addTechnicalDetail(`❌ Track subscription failed: ${trackSid} from ${participant.identity}`);
            if (client && client.handleSubscriptionError) {
                client.handleSubscriptionError(error, trackSid, participant.identity, 'event');
            }
        }

        function onTrackMuted(publication, participant) {
            console.log('*** TrackMuted EVENT ***', publication.kind, participant.identity);
            addTechnicalDetail(`🔇 Track muted: ${publication.kind} from ${participant.identity}`);
            const muteEvent = {
                type: 'muted',
                trackKind: publication.kind,
                participantIdentity: participant.identity,
                trackSid: publication.trackSid,
                timestamp: Date.now()
            };
            if (window.TestStateStore) {
                window.TestStateStore.mute.addEvent(muteEvent);
                window.TestStateStore.syncToWindow();
            } else {
                window.muteEvents = window.muteEvents || [];
                window.muteEvents.push(muteEvent);
            }
        }

        function onTrackUnmuted(publication, participant) {
            console.log('*** TrackUnmuted EVENT ***', publication.kind, participant.identity);
            addTechnicalDetail(`🔊 Track unmuted: ${publication.kind} from ${participant.identity}`);
            const unmuteEvent = {
                type: 'unmuted',
                trackKind: publication.kind,
                participantIdentity: participant.identity,
                trackSid: publication.trackSid,
                timestamp: Date.now()
            };
            if (window.TestStateStore) {
                window.TestStateStore.mute.addEvent(unmuteEvent);
                window.TestStateStore.syncToWindow();
            } else {
                window.muteEvents = window.muteEvents || [];
                window.muteEvents.push(unmuteEvent);
            }
        }

        function onTrackStreamStateChanged(publication, streamState, participant) {
            console.log('*** TrackStreamStateChanged EVENT ***', publication.kind, streamState, participant.identity);
            addTechnicalDetail(`🔄 Track stream state changed: ${publication.kind} from ${participant.identity} -> ${streamState}`);
            const stateEvent = {
                trackSid: publication.trackSid,
                trackKind: publication.kind,
                participantIdentity: participant.identity,
                streamState: streamState,
                timestamp: Date.now()
            };
            if (window.TestStateStore) {
                window.TestStateStore.trackStream.addStateEvent(stateEvent);
                window.TestStateStore.syncToWindow();
            } else {
                window.trackStreamStateEvents = window.trackStreamStateEvents || [];
                window.trackStreamStateEvents.push(stateEvent);
            }
        }

        function attach(room) {
            room.on(LiveKit.RoomEvent.TrackSubscribed, onTrackSubscribed);
            room.on(LiveKit.RoomEvent.TrackUnsubscribed, onTrackUnsubscribed);
            room.on(LiveKit.RoomEvent.TrackPublished, onTrackPublished);
            room.on(LiveKit.RoomEvent.TrackUnpublished, onTrackUnpublished);
            room.on(LiveKit.RoomEvent.TrackSubscriptionFailed, onTrackSubscriptionFailed);
            room.on(LiveKit.RoomEvent.TrackMuted, onTrackMuted);
            room.on(LiveKit.RoomEvent.TrackUnmuted, onTrackUnmuted);
            room.on(LiveKit.RoomEvent.TrackStreamStateChanged, onTrackStreamStateChanged);
        }

        return {
            onTrackSubscribed: onTrackSubscribed,
            onTrackUnsubscribed: onTrackUnsubscribed,
            onTrackPublished: onTrackPublished,
            onTrackUnpublished: onTrackUnpublished,
            onTrackSubscriptionFailed: onTrackSubscriptionFailed,
            onTrackMuted: onTrackMuted,
            onTrackUnmuted: onTrackUnmuted,
            onTrackStreamStateChanged: onTrackStreamStateChanged,
            attach: attach
        };
    }

    return {
        create: create
    };
})();

window.TrackEventHandlers = TrackEventHandlers;
