// LiveKit WebRTC Implementation
class LiveKitMeetClient {
    constructor() {
        this.room = null;
        this.localVideoTrack = null;
        this.localAudioTrack = null;
        this.connected = false;
        this.videoEnabled = true;
        this.audioEnabled = true;
        this.participants = new Map();
        
        this.initializeElements();
        this.setupEventListeners();
    }
    
    initializeElements() {
        this.joinForm = document.getElementById('joinForm');
        this.meetingRoom = document.getElementById('meetingRoom');
        this.meetingForm = document.getElementById('meetingForm');
        this.statusDiv = document.getElementById('status');
        
        // Form inputs
        this.liveKitUrlInput = document.getElementById('liveKitUrl');
        this.tokenInput = document.getElementById('token');
        this.roomNameInput = document.getElementById('roomName');
        this.participantNameInput = document.getElementById('participantName');
        
        // Meeting room elements
        this.roomTitle = document.getElementById('roomTitle');
        this.localVideo = document.getElementById('localVideo');
        this.remoteVideos = document.getElementById('remoteVideos');
        this.noVideoMessage = document.getElementById('noVideoMessage');
        this.connectionStatus = document.getElementById('connectionStatus');
        
        // Control buttons
        this.muteBtn = document.getElementById('muteBtn');
        this.cameraBtn = document.getElementById('cameraBtn');
        this.leaveBtn = document.getElementById('leaveBtn');
    }
    
    setupEventListeners() {
        this.meetingForm.addEventListener('submit', (e) => this.handleJoinMeeting(e));
        this.muteBtn.addEventListener('click', () => this.toggleMute());
        this.cameraBtn.addEventListener('click', () => this.toggleCamera());
        this.leaveBtn.addEventListener('click', () => this.leaveMeeting());
    }
    
    async handleJoinMeeting(e) {
        e.preventDefault();
        
        // Add big visual indicator that form was submitted
        document.getElementById('status').textContent = '🚀 FORM SUBMITTED - Starting connection process...';
        document.getElementById('status').className = 'status info';
        addTechnicalDetail('🚀 Form submission detected!');
        
        const liveKitUrl = this.liveKitUrlInput.value.trim();
        const token = this.tokenInput.value.trim();
        const roomName = this.roomNameInput.value.trim();
        const participantName = this.participantNameInput.value.trim();
        
        addTechnicalDetail(`Form values - URL: ${liveKitUrl}, Room: ${roomName}, Participant: ${participantName}, Token length: ${token.length}`);
        
        if (!liveKitUrl || !token || !roomName || !participantName) {
            this.showStatus('Please fill in all fields', 'error');
            addTechnicalDetail('❌ Missing required fields');
            return;
        }
        
        try {
            addTechnicalDetail('📞 Calling connectToRoom...');
            await this.connectToRoom(liveKitUrl, token, roomName, participantName);
        } catch (error) {
            console.error('Failed to join meeting:', error);
            addTechnicalDetail(`❌ Connection failed: ${error.message}`);
            this.showStatus(`Failed to join meeting: ${error.message}`, 'error');
        }
    }
    
    async connectToRoom(url, token, roomName, participantName) {
        markStepActive('step-room');
        this.showStatus('Creating room instance...', 'info');
        addTechnicalDetail(`Starting connection to: ${url}`);
        addTechnicalDetail(`Room: ${roomName}, Participant: ${participantName}`);
        
        try {
            // Create LiveKit room with fallback for video resolution
            const videoResolution = (LiveKit.VideoPresets && LiveKit.VideoPresets.h720 && LiveKit.VideoPresets.h720.resolution) 
                ? LiveKit.VideoPresets.h720.resolution 
                : { width: 1280, height: 720 };
            
            this.room = new LiveKit.Room({
                adaptiveStream: true,
                dynacast: true,
                videoCaptureDefaults: {
                    resolution: videoResolution,
                },
            });
            
            progressToNextStep('step-room', 'Room created');
            addTechnicalDetail(`✅ Room instance created with resolution: ${videoResolution.width}x${videoResolution.height}`);
            
            // Debug: Log available LiveKit events
            console.log('=== DEBUGGING LIVEKIT EVENTS ===');
            console.log('LiveKit object:', LiveKit);
            console.log('LiveKit.RoomEvent object:', LiveKit.RoomEvent);
            if (LiveKit.RoomEvent) {
                console.log('Available room events:', Object.keys(LiveKit.RoomEvent));
                addTechnicalDetail('Available events: ' + Object.keys(LiveKit.RoomEvent).join(', '));
            } else {
                console.error('LiveKit.RoomEvent is undefined!');
                addTechnicalDetail('❌ LiveKit.RoomEvent is undefined!');
            }
            console.log('Room object after creation:', this.room);
            console.log('Room constructor:', this.room.constructor.name);
            addTechnicalDetail('Room created: ' + this.room.constructor.name);
            console.log('=== END DEBUG ===');
            
            this.setupRoomEventListeners();
            
            // Connect to room first (before getting media)
            markStepActive('step-connect');
            this.showStatus('Connecting to LiveKit server...', 'info');
            addTechnicalDetail('Starting connection to LiveKit server...');
            
            console.log('=== CONNECTION ATTEMPT START ===');
            console.log('LiveKit server URL:', url);
            console.log('Token length:', token.length);
            console.log('Room object before connect:', this.room);
            console.log('LiveKit object type:', typeof LiveKit);
            console.log('LiveKit.Room constructor:', LiveKit.Room.toString().substring(0, 200));
            console.log('LiveKit has real Room events?', !!LiveKit.RoomEvent);
            
            addTechnicalDetail(`LiveKit server URL: ${url}`);
            addTechnicalDetail(`Token length: ${token.length} chars`);
            
            const connectStartTime = Date.now();
            window.connectionStartTime = connectStartTime;
            await this.room.connect(url, token);
            const connectEndTime = Date.now();
            
            console.log('room.connect() resolved in', connectEndTime - connectStartTime, 'ms');
            console.log('Room state after connect:', this.room.state);
            console.log('Room connection state:', this.room.engine?.connectionState);
            console.log('Room SID after connect:', this.room.sid);
            console.log('Is room actually connected?', this.room.state === 'connected');
            console.log('Room engine details:', {
                connectionState: this.room.engine?.connectionState,
                iceConnectionState: this.room.engine?.pcManager?.publisher?.iceConnectionState,
                subscriberIceState: this.room.engine?.pcManager?.subscriber?.iceConnectionState,
                signalingState: this.room.engine?.pcManager?.publisher?.signalingState
            });
            console.log('Available room states:', LiveKit.ConnectionState ? Object.keys(LiveKit.ConnectionState) : 'ConnectionState not available');
            console.log('=== CONNECTION ATTEMPT END ===');
            
            progressToNextStep('step-connect', `Connected in ${connectEndTime - connectStartTime}ms`);
            markStepActive('step-webrtc');
            addTechnicalDetail(`✅ room.connect() resolved in ${connectEndTime - connectStartTime}ms`);
            addTechnicalDetail(`Room state: ${this.room.state}`);
            addTechnicalDetail(`Room SID: ${this.room.sid}`);
            
            // Enable camera and microphone immediately after connection
            try {
                markStepActive('step-media');
                this.showStatus('Accessing camera and microphone...', 'info');
                addTechnicalDetail('Enabling camera and microphone after connection...');
                
                console.log('*** Calling enableCameraAndMicrophone() ***');
                await this.room.localParticipant.enableCameraAndMicrophone();
                
                console.log('Camera and microphone enabled successfully');
                console.log('Room state after media enabled:', this.room.state);
                console.log('Connection state after media:', this.room.engine?.connectionState);
                console.log('ICE states after media:', {
                    publisher: this.room.engine?.pcManager?.publisher?.iceConnectionState,
                    subscriber: this.room.engine?.pcManager?.subscriber?.iceConnectionState
                });
                addTechnicalDetail('✅ Camera and microphone enabled');
                progressToNextStep('step-media', 'Media enabled');
                
                // Get the tracks that were created
                const cameraPublication = this.room.localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
                const micPublication = this.room.localParticipant.getTrackPublication(LiveKit.Track.Source.Microphone);
                
                this.localVideoTrack = cameraPublication?.track;
                this.localAudioTrack = micPublication?.track;
                
                // Attach video to display
                if (this.localVideoTrack) {
                    this.localVideoTrack.attach(this.localVideo);
                    addTechnicalDetail('✅ Video track attached to display');
                    this.updateVideoDisplay();
                }
                
                console.log('All tracks published successfully');
                addTechnicalDetail('🎉 All tracks published successfully');
            } catch (publishError) {
                console.error('Failed to enable camera/microphone:', publishError);
                addTechnicalDetail(`❌ Failed to enable camera/microphone: ${publishError.message}`);
                markStepFailed('step-media', `Media error: ${publishError.message}`);
            }
            
            // Mark as connected based on successful room connection and media publishing
            this.connected = true;
            this.showMeetingRoom(roomName);
            
            // Verify WebRTC connection is established
            console.log('*** WebRTC Connection Verification ***');
            console.log('Room state:', this.room.state);
            console.log('Room SID:', this.room.sid || 'Not yet assigned');
            console.log('Connected participants:', this.room.remoteParticipants.size + 1);
            console.log('=== END CONNECTION VERIFICATION ===');
            
            // Connection is considered successful if room state is connected and tracks are published
            const isConnected = this.room.state === 'connected';
            
            if (isConnected) {
                console.log('*** WebRTC connection established successfully ***');
                
                addTechnicalDetail(`🎉 WebRTC connection established`);
                addTechnicalDetail(`Room state: ${this.room.state}`);
                addTechnicalDetail(`Room SID: ${this.room.sid || 'Pending assignment'}`);
                
                progressToNextStep('step-webrtc', 'WebRTC connected');
                markStepActive('step-complete');
                this.updateConnectionStatus('Connected');
                
                progressToNextStep('step-complete', 'Connection complete');
                this.showStatus(`Connected to room "${roomName}" as "${participantName}"`, 'success');
                addTechnicalDetail(`🎉 Successfully connected as ${participantName} to room ${roomName}`);
                
                // Set definitive flags for testing
                window.REAL_WEBRTC_CONNECTION_VERIFIED = true;
                window.connectionEstablished = true;
            } else {
                console.error('*** WebRTC connection failed ***', this.room.state);
                markStepFailed('step-webrtc', `Connection failed - room state: ${this.room.state}`);
                addTechnicalDetail(`❌ WebRTC connection failed - room state: ${this.room.state}`);
                window.REAL_WEBRTC_CONNECTION_VERIFIED = false;
                this.showStatus('Connection failed - unable to establish WebRTC connection', 'error');
            }
            
        } catch (error) {
            console.error('Connection failed with error:', error);
            console.error('Error details:', {
                message: error.message,
                name: error.name,
                stack: error.stack
            });
            addTechnicalDetail(`❌ Connection error: ${error.message}`);
            markStepFailed('step-connect', `Error: ${error.message}`);
            this.showStatus(`Connection failed: ${error.message}`, 'error');
            throw error;
        }
    }
    
    setupRoomEventListeners() {
        console.log('=== SETTING UP ROOM EVENT LISTENERS ===');
        console.log('Room object for event setup:', this.room);
        console.log('Setting up Connected event listener...');
        
        // Test if the event name exists
        const connectedEvent = LiveKit.RoomEvent ? LiveKit.RoomEvent.Connected : 'connected';
        console.log('Using Connected event:', connectedEvent);
        addTechnicalDetail('Using Connected event: ' + connectedEvent);
        
        this.room.on(connectedEvent, async () => {
            const connectionEndTime = Date.now();
            window.connectionTime = connectionEndTime - (window.connectionStartTime || connectionEndTime);
            
            console.log('*** Connected event fired ***');
            console.log('Connection time:', window.connectionTime, 'ms');
            addTechnicalDetail(`📡 Connected event fired after ${window.connectionTime}ms`);
        });
        
        this.room.on(LiveKit.RoomEvent ? LiveKit.RoomEvent.Disconnected : 'disconnected', () => {
            console.log('*** DISCONNECTED from room ***');
            addTechnicalDetail('⚠️ Disconnected from room');
            this.handleDisconnection();
        });
        
        this.room.on(LiveKit.RoomEvent.ConnectionQualityChanged, (quality, participant) => {
            console.log('Connection quality changed:', quality, participant?.identity);
            addTechnicalDetail(`Connection quality: ${quality} for ${participant?.identity || 'local'}`);
        });
        
        this.room.on(LiveKit.RoomEvent.Reconnecting, () => {
            console.log('*** RECONNECTING to room ***');
            addTechnicalDetail('🔄 Reconnecting to room...');
            updateStatus('Reconnecting...', 'info');
        });
        
        this.room.on(LiveKit.RoomEvent.Reconnected, () => {
            console.log('*** RECONNECTED to room ***');
            addTechnicalDetail('✅ Reconnected to room');
            updateStatus('Reconnected', 'success');
        });
        
        // Listen for any connection errors
        this.room.on('error', (error) => {
            console.error('*** ROOM ERROR ***', error);
            addTechnicalDetail(`❌ Room error: ${error.message || error}`);
            markStepFailed('step-webrtc', `Error: ${error.message || error}`);
        });
        
        // Listen for ICE connection state changes
        this.room.engine?.on('connectionStateChanged', (state) => {
            console.log('*** ENGINE CONNECTION STATE CHANGED ***', state);
            addTechnicalDetail(`Engine connection state: ${state}`);
        });
        
        // Listen for signaling state changes  
        this.room.engine?.on('signalingStateChanged', (state) => {
            console.log('*** ENGINE SIGNALING STATE CHANGED ***', state);
            addTechnicalDetail(`Signaling state: ${state}`);
        });
        
        // Listen for media errors
        this.room.on('mediaDevicesError', (error) => {
            console.error('*** MEDIA DEVICES ERROR ***', error);
            addTechnicalDetail(`❌ Media devices error: ${error.message || error}`);
        });
        
        this.room.on(LiveKit.RoomEvent.ParticipantConnected, (participant) => {
            console.log('Participant connected:', participant.identity);
            addTechnicalDetail(`👤 Participant connected: ${participant.identity}`);
            this.handleParticipantConnected(participant);
        });
        
        this.room.on(LiveKit.RoomEvent.ParticipantDisconnected, (participant) => {
            console.log('Participant disconnected:', participant.identity);
            addTechnicalDetail(`👤 Participant disconnected: ${participant.identity}`);
            this.handleParticipantDisconnected(participant);
        });
        
        this.room.on(LiveKit.RoomEvent.TrackSubscribed, (track, publication, participant) => {
            console.log('Track subscribed:', track.kind, participant.identity);
            addTechnicalDetail(`🎥 Track subscribed: ${track.kind} from ${participant.identity}`);
            this.handleTrackSubscribed(track, participant);
        });
        
        this.room.on(LiveKit.RoomEvent.TrackUnsubscribed, (track, publication, participant) => {
            console.log('Track unsubscribed:', track.kind, participant.identity);
            addTechnicalDetail(`🎥 Track unsubscribed: ${track.kind} from ${participant.identity}`);
            this.handleTrackUnsubscribed(track, participant);
        });
    }
    
    // OLD METHOD - Kept for reference. Now using room.localParticipant.enableCameraAndMicrophone()
    // async enableCameraAndMicrophone() {
    //     try {
    //         console.log('=== MEDIA ACQUISITION START ===');
    //         addTechnicalDetail('Starting media acquisition...');
    //         
    //         // Create local video track with fallback for video resolution
    //         const videoResolution = (LiveKit.VideoPresets && LiveKit.VideoPresets.h720 && LiveKit.VideoPresets.h720.resolution) 
    //             ? LiveKit.VideoPresets.h720.resolution 
    //             : { width: 1280, height: 720 };
    //         
    //         addTechnicalDetail(`Video resolution: ${videoResolution.width}x${videoResolution.height}`);
    //         
    //         console.log('Creating video track with resolution:', videoResolution);
    //         addTechnicalDetail('Creating video track...');
    //         this.localVideoTrack = await LiveKit.createLocalVideoTrack({
    //             resolution: videoResolution,
    //         });
    //         console.log('Video track created:', this.localVideoTrack);
    //         addTechnicalDetail('✅ Video track created');
    //         
    //         // Create local audio track
    //         console.log('Creating audio track...');
    //         addTechnicalDetail('Creating audio track...');
    //         this.localAudioTrack = await LiveKit.createLocalAudioTrack();
    //         console.log('Audio track created:', this.localAudioTrack);
    //         addTechnicalDetail('✅ Audio track created');
    //         
    //         // Attach local video to video element
    //         console.log('Attaching video track to element...');
    //         addTechnicalDetail('Attaching video track to display...');
    //         this.localVideoTrack.attach(this.localVideo);
    //         
    //         // Don't publish tracks yet - wait until after room connection
    //         console.log('Tracks created but not published yet - waiting for room connection');
    //         addTechnicalDetail('✅ Tracks created, waiting for room connection to publish');
    //         
    //         this.updateVideoDisplay();
    //         console.log('=== MEDIA ACQUISITION END ===');
    //         addTechnicalDetail('✅ Media acquisition completed');
    //         
    //     } catch (error) {
    //         console.error('=== MEDIA ACQUISITION FAILED ===');
    //         console.error('Failed to access camera/microphone:', error);
    //         console.error('Error name:', error.name);
    //         console.error('Error message:', error.message);
    //         console.error('Error stack:', error.stack);
    //         addTechnicalDetail(`❌ Media acquisition failed: ${error.message}`);
    //         markStepFailed('step-media', `Media error: ${error.message}`);
    //         this.showStatus('Failed to access camera/microphone. Please check permissions.', 'error');
    //         throw error;
    //     }
    // }
    
    handleParticipantConnected(participant) {
        const participantDiv = document.createElement('div');
        participantDiv.className = 'remote-video';
        participantDiv.id = `participant-${participant.identity}`;
        
        const nameDiv = document.createElement('div');
        nameDiv.className = 'participant-name';
        nameDiv.textContent = participant.identity;
        participantDiv.appendChild(nameDiv);
        
        this.remoteVideos.appendChild(participantDiv);
        this.participants.set(participant.identity, participantDiv);
    }
    
    handleParticipantDisconnected(participant) {
        const participantDiv = this.participants.get(participant.identity);
        if (participantDiv) {
            participantDiv.remove();
            this.participants.delete(participant.identity);
        }
    }
    
    handleTrackSubscribed(track, participant) {
        const participantDiv = this.participants.get(participant.identity);
        const videoKind = (LiveKit.Track && LiveKit.Track.Kind && LiveKit.Track.Kind.Video) 
            ? LiveKit.Track.Kind.Video 
            : 'video';
        if (participantDiv && track.kind === videoKind) {
            const videoElement = document.createElement('video');
            videoElement.autoplay = true;
            videoElement.playsInline = true;
            videoElement.style.width = '100%';
            videoElement.style.height = '100%';
            videoElement.style.objectFit = 'cover';
            
            track.attach(videoElement);
            participantDiv.insertBefore(videoElement, participantDiv.firstChild);
        }
    }
    
    handleTrackUnsubscribed(track, participant) {
        track.detach();
    }
    
    async toggleMute() {
        if (!this.localAudioTrack) return;
        
        this.audioEnabled = !this.audioEnabled;
        this.localAudioTrack.setEnabled(this.audioEnabled);
        
        this.muteBtn.textContent = this.audioEnabled ? 'Mute' : 'Unmute';
        this.muteBtn.className = this.audioEnabled ? 'control-btn mute-btn' : 'control-btn mute-btn muted';
    }
    
    async toggleCamera() {
        if (!this.localVideoTrack) return;
        
        this.videoEnabled = !this.videoEnabled;
        this.localVideoTrack.setEnabled(this.videoEnabled);
        
        this.updateVideoDisplay();
        this.cameraBtn.textContent = this.videoEnabled ? 'Camera Off' : 'Camera On';
        this.cameraBtn.className = this.videoEnabled ? 'control-btn camera-btn' : 'control-btn camera-btn off';
    }
    
    updateVideoDisplay() {
        if (this.videoEnabled) {
            this.localVideo.style.display = 'block';
            this.noVideoMessage.style.display = 'none';
        } else {
            this.localVideo.style.display = 'none';
            this.noVideoMessage.style.display = 'flex';
        }
    }
    
    async leaveMeeting() {
        try {
            if (this.room) {
                await this.room.disconnect();
            }
            this.handleDisconnection();
        } catch (error) {
            console.error('Error leaving meeting:', error);
            this.handleDisconnection();
        }
    }
    
    handleDisconnection() {
        this.connected = false;
        
        // Clean up tracks
        if (this.localVideoTrack) {
            this.localVideoTrack.stop();
            this.localVideoTrack = null;
        }
        if (this.localAudioTrack) {
            this.localAudioTrack.stop();
            this.localAudioTrack = null;
        }
        
        // Clean up room
        this.room = null;
        
        // Clear participants
        this.participants.clear();
        this.remoteVideos.innerHTML = '';
        
        // Reset UI
        this.showJoinForm();
        this.showStatus('Left the meeting', 'info');
        
        // Reset button states
        this.muteBtn.textContent = 'Mute';
        this.muteBtn.className = 'control-btn mute-btn';
        this.cameraBtn.textContent = 'Camera Off';
        this.cameraBtn.className = 'control-btn camera-btn';
        this.audioEnabled = true;
        this.videoEnabled = true;
    }
    
    showMeetingRoom(roomName) {
        this.joinForm.style.display = 'none';
        this.meetingRoom.style.display = 'flex';
        this.roomTitle.textContent = roomName;
    }
    
    showJoinForm() {
        this.meetingRoom.style.display = 'none';
        this.joinForm.style.display = 'block';
    }
    
    showStatus(message, type = 'info') {
        this.statusDiv.textContent = message;
        this.statusDiv.className = `status ${type}`;
        this.statusDiv.style.display = 'block';
        
        if (type !== 'error') {
            setTimeout(() => {
                this.statusDiv.style.display = 'none';
            }, 5000);
        }
    }
    
    updateConnectionStatus(status) {
        this.connectionStatus.textContent = `Status: ${status}`;
        this.connectionStatus.className = 'status success';
        
        // Also update a simple flag for easier test automation
        window.connectionEstablished = (status === 'Connected');
    }
    
    // Public API for test automation
    isConnected() {
        const connectedState = (LiveKit.ConnectionState && LiveKit.ConnectionState.Connected) 
            ? LiveKit.ConnectionState.Connected 
            : 'connected';
        return this.connected && this.room?.state === connectedState;
    }
    
    isInMeetingRoom() {
        return this.meetingRoom.style.display !== 'none';
    }
    
    getCurrentRoomName() {
        return this.roomTitle.textContent;
    }
    
    isJoinFormVisible() {
        return this.joinForm.style.display !== 'none';
    }
    
    getParticipantCount() {
        return this.participants.size + (this.connected ? 1 : 0); // +1 for local participant
    }
}