var LiveKitTestHelpers = {
    isLiveKitLoaded: function() {
        return typeof LiveKit !== 'undefined';
    },

    getLastError: function() {
        return window.lastError || '';
    },

    isUsingMock: function() {
        return !!window.USING_MOCK_LIVEKIT;
    },

    isRealWebRTCConnectionVerified: function() {
        return window.REAL_WEBRTC_CONNECTION_VERIFIED || false;
    },

    isConnectionEstablished: function() {
        return window.connectionEstablished || false;
    },

    isClientConnected: function() {
        return window.liveKitClient && window.liveKitClient.isConnected();
    },

    getConnectionTime: function() {
        return window.connectionTime || 0;
    },

    getConsoleLogs: function() {
        return window.consoleLogCapture ? window.consoleLogCapture.join('\n') : 'No console logs captured';
    },

    isInMeetingRoom: function() {
        return window.liveKitClient && window.liveKitClient.isInMeetingRoom();
    },

    isScreenSharing: function() {
        return window.liveKitClient && window.liveKitClient.isScreenSharing() || false;
    },

    isScreenSharePermissionDenied: function() {
        return window.screenSharePermissionDenied || false;
    },

    getLastScreenShareError: function() {
        return window.lastScreenShareError || 'No error captured';
    },

    enableSimulcast: function() {
        window.simulcastEnabled = true;
        if (window.liveKitClient) window.liveKitClient.setSimulcastEnabled(true);
    },

    disableSimulcast: function() {
        window.simulcastEnabled = false;
        if (window.liveKitClient) window.liveKitClient.setSimulcastEnabled(false);
    },

    isSimulcastEnabled: function() {
        return window.liveKitClient ? window.liveKitClient.isSimulcastEnabled() : (window.simulcastEnabled !== false);
    },

    setVideoQualityPreference: function(quality) {
        window.currentVideoQuality = quality;
        if (window.liveKitClient) window.liveKitClient.setVideoQualityPreference(quality);
    },

    getVideoQualityPreference: function() {
        return window.liveKitClient ? window.liveKitClient.getVideoQualityPreference() : (window.currentVideoQuality || 'HIGH');
    },

    setMaxReceiveBandwidth: function(kbps) {
        if (window.liveKitClient) window.liveKitClient.setMaxReceiveBandwidth(kbps);
    },

    getRemoteVideoTrackWidthByPublisher: function(publisherIdentity) {
        if (!window.liveKitClient) return 0;
        if (!window.liveKitClient.room) return 0;
        var participantDiv = document.getElementById('participant-' + publisherIdentity);
        if (participantDiv) {
            var video = participantDiv.querySelector('video');
            if (video && video.videoWidth > 0) {
                return video.videoWidth;
            }
        }
        var remoteParticipants = window.liveKitClient.room.remoteParticipants;
        var width = 0;
        remoteParticipants.forEach(function(p) {
            if (p.identity === publisherIdentity) {
                p.trackPublications.forEach(function(pub) {
                    if (pub.kind === 'video' && pub.track) {
                        if (pub.track.dimensions && pub.track.dimensions.width > 0) {
                            width = pub.track.dimensions.width;
                        } else if (pub.track.mediaStreamTrack) {
                            var settings = pub.track.mediaStreamTrack.getSettings();
                            if (settings.width) width = settings.width;
                        }
                    }
                });
            }
        });
        return width;
    },

    getRemoteVideoTracks: function() {
        return window.liveKitClient ? window.liveKitClient.getRemoteVideoTracks() : [];
    },

    getReceivingLayerInfo: function(publisherIdentity) {
        return window.liveKitClient ? window.liveKitClient.getReceivingLayerInfo(publisherIdentity) : null;
    },

    getSubscriptionFailedEventCount: function() {
        return window.subscriptionFailedEvents ? window.subscriptionFailedEvents.length : 0;
    },

    isSubscriptionPermissionDenied: function() {
        return window.subscriptionPermissionDenied || false;
    },

    getLastSubscriptionError: function() {
        return window.lastSubscriptionError || '';
    },

    getPlayingVideoElementCount: function() {
        try {
            return Array.from(document.querySelectorAll('video')).filter(function(v) {
                return v.videoWidth > 0 && v.videoHeight > 0 && !v.paused && !v.ended && v.readyState >= 2;
            }).length;
        } catch(e) {
            return 0;
        }
    },

    getSubscribedVideoTrackCount: function() {
        try {
            if (!window.liveKitClient || !window.liveKitClient.room) return 0;
            return Array.from(window.liveKitClient.room.tracks.values()).filter(function(t) {
                return t.kind === 'video' && t.isSubscribed;
            }).length;
        } catch(e) {
            return 0;
        }
    },

    muteAudio: function() {
        if (window.liveKitClient) window.liveKitClient.muteAudio();
    },

    unmuteAudio: function() {
        if (window.liveKitClient) window.liveKitClient.unmuteAudio();
    },

    muteVideo: function() {
        if (window.liveKitClient) window.liveKitClient.muteVideo();
    },

    unmuteVideo: function() {
        if (window.liveKitClient) window.liveKitClient.unmuteVideo();
    },

    isAudioMuted: function() {
        return window.liveKitClient ? window.liveKitClient.isAudioMuted() : true;
    },

    isVideoMuted: function() {
        return window.liveKitClient ? window.liveKitClient.isVideoMuted() : true;
    },

    getLocalAudioTrackState: function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var pub = window.liveKitClient.room.localParticipant.getTrackPublication(LiveKit.Track.Source.Microphone);
        if (!pub || !pub.track) return null;
        return {
            muted: pub.isMuted,
            enabled: pub.track.isEnabled,
            sid: pub.trackSid
        };
    },

    getLocalVideoTrackState: function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var pub = window.liveKitClient.room.localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
        if (!pub || !pub.track) return null;
        return {
            muted: pub.isMuted,
            enabled: pub.track.isEnabled,
            sid: pub.trackSid
        };
    },

    getRemoteParticipantTrackMuteState: function(participantIdentity, trackType) {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var participant = null;
        window.liveKitClient.room.remoteParticipants.forEach(function(p) {
            if (p.identity === participantIdentity) participant = p;
        });
        if (!participant) return null;
        var result = null;
        participant.trackPublications.forEach(function(pub) {
            if (pub.kind === trackType) {
                result = {
                    muted: pub.isMuted,
                    enabled: pub.track ? pub.track.isEnabled : false,
                    sid: pub.trackSid
                };
            }
        });
        return result;
    },

    getMuteEventCount: function() {
        return window.muteEvents ? window.muteEvents.length : 0;
    },

    getLastMuteEvent: function() {
        return window.muteEvents && window.muteEvents.length > 0
            ? window.muteEvents[window.muteEvents.length - 1]
            : null;
    },

    clearMuteEvents: function() {
        window.muteEvents = [];
    },

    sendDataMessage: function(message, reliable, destinationIdentities) {
        if (!window.liveKitClient || !window.liveKitClient.room) {
            window.lastDataChannelError = 'No active room connection';
            window.dataPublishingBlocked = true;
            return false;
        }
        var permissions = window.liveKitClient.room.localParticipant.permissions;
        if (permissions && permissions.canPublishData === false) {
            window.lastDataChannelError = 'Permission denied: canPublishData is false';
            window.dataPublishingBlocked = true;
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
                    window.dataMessagesSent = window.dataMessagesSent || [];
                    window.dataMessagesSent.push({
                        content: message,
                        reliable: reliable !== false,
                        destinationIdentities: destinationIdentities || null,
                        timestamp: Date.now(),
                        size: data.length
                    });
                })
                .catch(function(e) {
                    console.error('Async error sending data message:', e);
                    window.lastDataChannelError = e.message || e.toString();
                    var errorMsg = (e.message || e.toString()).toLowerCase();
                    if (errorMsg.includes('permission') || errorMsg.includes('denied') ||
                        errorMsg.includes('forbidden') || errorMsg.includes('unauthorized') ||
                        errorMsg.includes('not allowed')) {
                        window.dataPublishingBlocked = true;
                    }
                });
            return true;
        } catch (e) {
            console.error('Failed to send data message:', e);
            window.lastDataChannelError = e.message || e.toString();
            var errorMsg = (e.message || e.toString()).toLowerCase();
            if (errorMsg.includes('permission') || errorMsg.includes('denied') ||
                errorMsg.includes('forbidden') || errorMsg.includes('unauthorized') ||
                errorMsg.includes('not allowed')) {
                window.dataPublishingBlocked = true;
            }
            return false;
        }
    },

    sendDataMessageOfSize: function(sizeBytes, reliable) {
        var message = 'X'.repeat(sizeBytes);
        var encoder = new TextEncoder();
        var actualSize = encoder.encode(message).length;
        if (actualSize !== sizeBytes) {
            console.warn('Message size mismatch: requested ' + sizeBytes + ', got ' + actualSize);
        }
        return this.sendDataMessage(message, reliable, null);
    },

    sendTimestampedDataMessage: function(message, reliable) {
        var timestampedMessage = JSON.stringify({
            content: message,
            timestamp: Date.now()
        });
        return this.sendDataMessage(timestampedMessage, reliable, null);
    },

    getReceivedDataMessages: function() {
        return window.dataChannelMessages || [];
    },

    getReceivedDataMessageCount: function() {
        return window.dataChannelMessages ? window.dataChannelMessages.length : 0;
    },

    findReceivedDataMessage: function(expectedContent, fromIdentity) {
        var messages = window.dataChannelMessages || [];
        for (var i = 0; i < messages.length; i++) {
            if (messages[i].content === expectedContent) {
                if (!fromIdentity || messages[i].from === fromIdentity) {
                    return messages[i];
                }
            }
        }
        return null;
    },

    hasReceivedDataMessage: function(expectedContent, fromIdentity) {
        return this.findReceivedDataMessage(expectedContent, fromIdentity) !== null;
    },

    getDataMessagesFromSender: function(senderIdentity) {
        var messages = window.dataChannelMessages || [];
        return messages.filter(function(msg) {
            return msg.from === senderIdentity;
        });
    },

    isDataPublishingBlocked: function() {
        return window.dataPublishingBlocked || false;
    },

    getLastDataChannelError: function() {
        return window.lastDataChannelError || '';
    },

    getDataChannelLatencyStats: function() {
        var messages = window.dataChannelMessages || [];
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
    },

    clearDataChannelState: function() {
        window.dataChannelMessages = [];
        window.dataMessagesSent = [];
        window.lastDataChannelError = '';
        window.dataPublishingBlocked = false;
    },

    getSentDataMessageCount: function() {
        return window.dataMessagesSent ? window.dataMessagesSent.length : 0;
    },

    isDynacastEnabled: function() {
        return window.liveKitClient && window.liveKitClient.isDynacastEnabled() || false;
    },

    getTrackStreamState: function(publisherIdentity) {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var participant = null;
        window.liveKitClient.room.remoteParticipants.forEach(function(p) {
            if (p.identity === publisherIdentity) participant = p;
        });
        if (!participant) return null;
        var streamState = null;
        participant.trackPublications.forEach(function(pub) {
            if (pub.kind === 'video') {
                if (!pub.subscribed) {
                    streamState = 'unsubscribed';
                } else if (pub.track) {
                    streamState = pub.track.streamState || 'active';
                } else {
                    streamState = 'pending';
                }
            }
        });
        return streamState;
    },

    isVideoSubscribed: function(publisherIdentity) {
        if (!window.liveKitClient || !window.liveKitClient.room) return false;
        var participant = null;
        window.liveKitClient.room.remoteParticipants.forEach(function(p) {
            if (p.identity === publisherIdentity) participant = p;
        });
        if (!participant) return false;
        var subscribed = false;
        participant.trackPublications.forEach(function(pub) {
            if (pub.kind === 'video' && pub.subscribed) {
                subscribed = true;
            }
        });
        return subscribed;
    },

    setVideoSubscribed: function(publisherIdentity, subscribed) {
        window.lastSubscriptionError = null;
        if (!window.liveKitClient || !window.liveKitClient.room) {
            window.lastSubscriptionError = 'No room connected';
            return false;
        }
        var participant = null;
        window.liveKitClient.room.remoteParticipants.forEach(function(p) {
            if (p.identity === publisherIdentity) participant = p;
        });
        if (!participant) {
            window.lastSubscriptionError = 'Participant not found: ' + publisherIdentity;
            return false;
        }
        var success = false;
        participant.trackPublications.forEach(function(pub) {
            if (pub.kind === 'video') {
                try {
                    pub.setSubscribed(subscribed);
                    success = true;
                    console.log('Set video subscription for', publisherIdentity, 'to', subscribed);
                } catch (e) {
                    window.lastSubscriptionError = e.message || String(e);
                    console.error('Failed to set subscription:', e);
                }
            }
        });
        if (!success && !window.lastSubscriptionError) {
            window.lastSubscriptionError = 'No video track found for: ' + publisherIdentity;
        }
        return success;
    },

    clearDynacastState: function() {
        window.trackStreamStateEvents = [];
    },

    capturePublisherVideoBitrate: async function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return false;
        var localParticipant = window.liveKitClient.room.localParticipant;
        if (!localParticipant) return false;

        var cameraPub = localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
        if (!cameraPub || !cameraPub.track) return false;

        var sender = cameraPub.track.sender;
        if (!sender) return false;

        try {
            var stats = await sender.getStats();
            var totalBytesSent = 0;
            stats.forEach(function(report) {
                if (report.type === 'outbound-rtp' && report.kind === 'video') {
                    totalBytesSent += report.bytesSent || 0;
                }
            });
            window.baselineVideoBytesCapture = {
                bytes: totalBytesSent,
                timestamp: Date.now()
            };
            return true;
        } catch (e) {
            console.error('Failed to capture video bitrate:', e);
            return false;
        }
    },

    getPublisherVideoBitrateKbps: async function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return 0;
        var localParticipant = window.liveKitClient.room.localParticipant;
        if (!localParticipant) return 0;

        var cameraPub = localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
        if (!cameraPub || !cameraPub.track) return 0;

        var sender = cameraPub.track.sender;
        if (!sender) return 0;

        try {
            var stats = await sender.getStats();
            var totalBytesSent = 0;
            stats.forEach(function(report) {
                if (report.type === 'outbound-rtp' && report.kind === 'video') {
                    totalBytesSent += report.bytesSent || 0;
                }
            });

            if (!window.baselineVideoBytesCapture) return 0;

            var bytesDelta = totalBytesSent - window.baselineVideoBytesCapture.bytes;
            var timeDeltaMs = Date.now() - window.baselineVideoBytesCapture.timestamp;
            if (timeDeltaMs <= 0) return 0;

            var bitsPerSecond = (bytesDelta * 8) / (timeDeltaMs / 1000);
            return Math.round(bitsPerSecond / 1000);
        } catch (e) {
            console.error('Failed to get video bitrate:', e);
            return 0;
        }
    },

    measureVideoBitrateOverInterval: async function(intervalMs) {
        if (!window.liveKitClient || !window.liveKitClient.room) return 0;
        var localParticipant = window.liveKitClient.room.localParticipant;
        if (!localParticipant) return 0;

        var cameraPub = localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
        if (!cameraPub || !cameraPub.track) return 0;

        var sender = cameraPub.track.sender;
        if (!sender) return 0;

        try {
            var startStats = await sender.getStats();
            var startBytes = 0;
            startStats.forEach(function(report) {
                if (report.type === 'outbound-rtp' && report.kind === 'video') {
                    startBytes += report.bytesSent || 0;
                }
            });
            var startTime = Date.now();

            await new Promise(function(resolve) { setTimeout(resolve, intervalMs); });

            var endStats = await sender.getStats();
            var endBytes = 0;
            endStats.forEach(function(report) {
                if (report.type === 'outbound-rtp' && report.kind === 'video') {
                    endBytes += report.bytesSent || 0;
                }
            });
            var endTime = Date.now();

            var bytesDelta = endBytes - startBytes;
            var timeDeltaMs = endTime - startTime;
            if (timeDeltaMs <= 0) return 0;

            var bitsPerSecond = (bytesDelta * 8) / (timeDeltaMs / 1000);
            return Math.round(bitsPerSecond / 1000);
        } catch (e) {
            console.error('Failed to measure video bitrate:', e);
            return 0;
        }
    },

    startListeningForRoomMetadataEvents: function() {
    },

    startListeningForParticipantMetadataEvents: function() {
    },

    getRoomMetadataEvents: function() {
        return window.roomMetadataEvents || [];
    },

    getParticipantMetadataEvents: function() {
        return window.participantMetadataEvents || [];
    },

    getRoomMetadataEventCount: function() {
        return window.roomMetadataEvents ? window.roomMetadataEvents.length : 0;
    },

    getParticipantMetadataEventCount: function() {
        return window.participantMetadataEvents ? window.participantMetadataEvents.length : 0;
    },

    getCurrentRoomMetadata: function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return '';
        return window.liveKitClient.room.metadata || '';
    },

    getParticipantMetadata: function(identity) {
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
    },

    getLocalParticipantMetadata: function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return '';
        var localParticipant = window.liveKitClient.room.localParticipant;
        return localParticipant ? (localParticipant.metadata || '') : '';
    },

    waitForRoomMetadataEvent: function(expectedValue, timeoutSeconds) {
        var events = window.roomMetadataEvents || [];
        for (var i = 0; i < events.length; i++) {
            if (events[i].metadata === expectedValue) return true;
        }
        return false;
    },

    waitForParticipantMetadataEvent: function(identity, expectedValue, timeoutSeconds) {
        var events = window.participantMetadataEvents || [];
        for (var i = 0; i < events.length; i++) {
            if (events[i].participantIdentity === identity && events[i].metadata === expectedValue) {
                return true;
            }
        }
        return false;
    },

    hasRoomMetadataEventWithValue: function(expectedValue) {
        var events = window.roomMetadataEvents || [];
        for (var i = 0; i < events.length; i++) {
            if (events[i].metadata === expectedValue) return true;
        }
        return false;
    },

    hasParticipantMetadataEventFor: function(identity, expectedValue) {
        var events = window.participantMetadataEvents || [];
        for (var i = 0; i < events.length; i++) {
            if (events[i].participantIdentity === identity && events[i].metadata === expectedValue) {
                return true;
            }
        }
        return false;
    },

    clearMetadataEvents: function() {
        window.roomMetadataEvents = [];
        window.participantMetadataEvents = [];
    }
};

window.LiveKitTestHelpers = LiveKitTestHelpers;
