var BitrateHelpers = (function() {
    function capturePublisherVideoBitrate() {
        return new Promise(function(resolve) {
            if (!window.liveKitClient || !window.liveKitClient.room) {
                resolve(false);
                return;
            }
            var localParticipant = window.liveKitClient.room.localParticipant;
            if (!localParticipant) {
                resolve(false);
                return;
            }

            var cameraPub = localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
            if (!cameraPub || !cameraPub.track) {
                resolve(false);
                return;
            }

            var sender = cameraPub.track.sender;
            if (!sender) {
                resolve(false);
                return;
            }

            sender.getStats().then(function(stats) {
                var totalBytesSent = 0;
                stats.forEach(function(report) {
                    if (report.type === 'outbound-rtp' && report.kind === 'video') {
                        totalBytesSent += report.bytesSent || 0;
                    }
                });
                var capture = {
                    bytes: totalBytesSent,
                    timestamp: Date.now()
                };
                if (window.TestStateStore) {
                    window.TestStateStore.bitrate.setBaselineCapture(capture);
                }
                window.baselineVideoBytesCapture = capture;
                resolve(true);
            }).catch(function(e) {
                console.error('Failed to capture video bitrate:', e);
                resolve(false);
            });
        });
    }

    function getPublisherVideoBitrateKbps() {
        return new Promise(function(resolve) {
            if (!window.liveKitClient || !window.liveKitClient.room) {
                resolve(0);
                return;
            }
            var localParticipant = window.liveKitClient.room.localParticipant;
            if (!localParticipant) {
                resolve(0);
                return;
            }

            var cameraPub = localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
            if (!cameraPub || !cameraPub.track) {
                resolve(0);
                return;
            }

            var sender = cameraPub.track.sender;
            if (!sender) {
                resolve(0);
                return;
            }

            var baseline = window.TestStateStore
                ? window.TestStateStore.bitrate.getBaselineCapture()
                : window.baselineVideoBytesCapture;

            if (!baseline) {
                resolve(0);
                return;
            }

            sender.getStats().then(function(stats) {
                var totalBytesSent = 0;
                stats.forEach(function(report) {
                    if (report.type === 'outbound-rtp' && report.kind === 'video') {
                        totalBytesSent += report.bytesSent || 0;
                    }
                });

                var bytesDelta = totalBytesSent - baseline.bytes;
                var timeDeltaMs = Date.now() - baseline.timestamp;
                if (timeDeltaMs <= 0) {
                    resolve(0);
                    return;
                }

                var bitsPerSecond = (bytesDelta * 8) / (timeDeltaMs / 1000);
                resolve(Math.round(bitsPerSecond / 1000));
            }).catch(function(e) {
                console.error('Failed to get video bitrate:', e);
                resolve(0);
            });
        });
    }

    function measureVideoBitrateOverInterval(intervalMs) {
        return new Promise(function(resolve) {
            if (!window.liveKitClient || !window.liveKitClient.room) {
                resolve(0);
                return;
            }
            var localParticipant = window.liveKitClient.room.localParticipant;
            if (!localParticipant) {
                resolve(0);
                return;
            }

            var cameraPub = localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
            if (!cameraPub || !cameraPub.track) {
                resolve(0);
                return;
            }

            var sender = cameraPub.track.sender;
            if (!sender) {
                resolve(0);
                return;
            }

            var startBytes = 0;
            var startTime = Date.now();

            sender.getStats().then(function(startStats) {
                startStats.forEach(function(report) {
                    if (report.type === 'outbound-rtp' && report.kind === 'video') {
                        startBytes += report.bytesSent || 0;
                    }
                });

                setTimeout(function() {
                    sender.getStats().then(function(endStats) {
                        var endBytes = 0;
                        endStats.forEach(function(report) {
                            if (report.type === 'outbound-rtp' && report.kind === 'video') {
                                endBytes += report.bytesSent || 0;
                            }
                        });
                        var endTime = Date.now();

                        var bytesDelta = endBytes - startBytes;
                        var timeDeltaMs = endTime - startTime;
                        if (timeDeltaMs <= 0) {
                            resolve(0);
                            return;
                        }

                        var bitsPerSecond = (bytesDelta * 8) / (timeDeltaMs / 1000);
                        resolve(Math.round(bitsPerSecond / 1000));
                    }).catch(function(e) {
                        console.error('Failed to measure video bitrate:', e);
                        resolve(0);
                    });
                }, intervalMs);
            }).catch(function(e) {
                console.error('Failed to measure video bitrate:', e);
                resolve(0);
            });
        });
    }

    return {
        capturePublisherVideoBitrate: capturePublisherVideoBitrate,
        getPublisherVideoBitrateKbps: getPublisherVideoBitrateKbps,
        measureVideoBitrateOverInterval: measureVideoBitrateOverInterval
    };
})();

window.BitrateHelpers = BitrateHelpers;
