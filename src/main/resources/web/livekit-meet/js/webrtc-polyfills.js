// WebRTC polyfills for containerized environments
function setupWebRTCPolyfills() {
    markStepActive('step-polyfill');
    updateStatus('Setting up WebRTC polyfills...', 'info');
    addTechnicalDetail('Starting WebRTC polyfill setup');
    
    console.log('=== WEBRTC POLYFILL SETUP ===');
    console.log('navigator.mediaDevices exists:', !!navigator.mediaDevices);
    console.log('getUserMedia exists:', !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia));
    console.log('RTCPeerConnection exists:', !!window.RTCPeerConnection);
    console.log('RTCDataChannel exists:', !!window.RTCDataChannel);
    console.log('RTCSessionDescription exists:', !!window.RTCSessionDescription);
    console.log('RTCIceCandidate exists:', !!window.RTCIceCandidate);
    
    addTechnicalDetail('Checking browser WebRTC capabilities...');
    addTechnicalDetail(`navigator.mediaDevices: ${!!navigator.mediaDevices}`);
    addTechnicalDetail(`getUserMedia: ${!!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)}`);
    addTechnicalDetail(`RTCPeerConnection: ${!!window.RTCPeerConnection}`);
    addTechnicalDetail(`RTCDataChannel: ${!!window.RTCDataChannel}`);
    
    // Polyfill for containerized browsers that might not have mediaDevices
    if (!navigator.mediaDevices) {
        console.warn('navigator.mediaDevices not found, creating polyfill');
        addTechnicalDetail('⚠️ navigator.mediaDevices missing - creating polyfill');
        navigator.mediaDevices = {};
    }
    
    if (!navigator.mediaDevices.getUserMedia) {
        console.warn('getUserMedia not found, creating mock implementation');
        addTechnicalDetail('⚠️ getUserMedia missing - creating mock implementation');
        updateStatus('Creating mock camera/microphone...', 'info');
        navigator.mediaDevices.getUserMedia = function(constraints) {
            console.log('Mock getUserMedia called with constraints:', constraints);
            
            // Create mock video track
            const canvas = document.createElement('canvas');
            canvas.width = 640;
            canvas.height = 480;
            const ctx = canvas.getContext('2d');
            
            // Draw a simple test pattern
            ctx.fillStyle = '#4CAF50';
            ctx.fillRect(0, 0, canvas.width, canvas.height);
            ctx.fillStyle = '#fff';
            ctx.font = '30px Arial';
            ctx.textAlign = 'center';
            ctx.fillText('Mock Video', canvas.width / 2, canvas.height / 2);
            
            const stream = canvas.captureStream(30);
            
            // Add mock audio track if needed
            if (constraints && constraints.audio) {
                try {
                    const audioContext = new (window.AudioContext || window.webkitAudioContext)();
                    const oscillator = audioContext.createOscillator();
                    const dest = audioContext.createMediaStreamDestination();
                    oscillator.connect(dest);
                    oscillator.frequency.value = 440; // A4 note
                    oscillator.start();
                    
                    // Add audio track to stream
                    const audioTrack = dest.stream.getAudioTracks()[0];
                    if (audioTrack) {
                        stream.addTrack(audioTrack);
                    }
                } catch (e) {
                    console.warn('Failed to create audio track:', e);
                }
            }
            
            // Enhance the stream with missing methods that LiveKit might expect
            enhanceStreamWithEventListeners(stream);
            
            // Also enhance the tracks with missing methods
            stream.getTracks().forEach(track => {
                enhanceTrackWithEventListeners(track);
            });
            
            console.log('Mock getUserMedia returning stream with tracks:', stream.getTracks().length);
            return Promise.resolve(stream);
        };
    }
    
    // Enhance RTCPeerConnection with addEventListener polyfill
    if (window.RTCPeerConnection) {
        const originalRTCPeerConnection = window.RTCPeerConnection;
        window.RTCPeerConnection = function(...args) {
            const pc = new originalRTCPeerConnection(...args);
            enhanceObjectWithEventListeners(pc, 'RTCPeerConnection');
            return pc;
        };
        Object.setPrototypeOf(window.RTCPeerConnection, originalRTCPeerConnection);
        copyStaticProperties(window.RTCPeerConnection, originalRTCPeerConnection);
    }
    
    // Enhance RTCDataChannel with addEventListener polyfill
    if (window.RTCDataChannel) {
        const originalRTCDataChannel = window.RTCDataChannel;
        window.RTCDataChannel = function(...args) {
            const dc = new originalRTCDataChannel(...args);
            enhanceObjectWithEventListeners(dc, 'RTCDataChannel');
            return dc;
        };
        Object.setPrototypeOf(window.RTCDataChannel, originalRTCDataChannel);
        copyStaticProperties(window.RTCDataChannel, originalRTCDataChannel);
    }
    
    // Add a safer global addEventListener interceptor
    setupSaferGlobalEventListenerInterceptor();
    
    console.log('=== WEBRTC POLYFILL COMPLETE ===');
    addTechnicalDetail('✅ WebRTC polyfill setup completed');
    progressToNextStep('step-polyfill', 'Polyfills ready');
}

function enhanceStreamWithEventListeners(stream) {
    if (stream && typeof stream.addEventListener !== 'function') {
        addEventListenerPolyfill(stream, 'MediaStream');
    }
}

function enhanceTrackWithEventListeners(track) {
    if (track && typeof track.addEventListener !== 'function') {
        addEventListenerPolyfill(track, 'MediaStreamTrack');
    }
}

function enhanceObjectWithEventListeners(obj, objectName) {
    if (obj && typeof obj.addEventListener !== 'function') {
        addEventListenerPolyfill(obj, objectName);
    }
}

function addEventListenerPolyfill(obj, objectName) {
    // Prevent infinite recursion and double-polyfilling
    if (!obj || obj._eventListeners || obj._polyfillApplied) {
        return;
    }
    
    // Mark this object as being polyfilled to prevent recursion
    obj._polyfillApplied = true;
    obj._eventListeners = {};
    
    // Use Object.defineProperty to avoid triggering getters
    Object.defineProperty(obj, 'addEventListener', {
        value: function(event, callback) {
            console.log(`${objectName} addEventListener called:`, event);
            if (!obj._eventListeners[event]) {
                obj._eventListeners[event] = [];
            }
            obj._eventListeners[event].push(callback);
            
            // Try to use the native on* property if available
            const eventProp = 'on' + event;
            if (eventProp in obj && typeof obj[eventProp] !== 'function') {
                obj[eventProp] = function(e) {
                    if (obj._eventListeners[event]) {
                        obj._eventListeners[event].forEach(cb => {
                            try {
                                cb(e);
                            } catch (err) {
                                console.error(`Error in ${objectName} event callback:`, err);
                            }
                        });
                    }
                };
            }
        },
        enumerable: false,
        writable: true,
        configurable: true
    });
    
    Object.defineProperty(obj, 'removeEventListener', {
        value: function(event, callback) {
            console.log(`${objectName} removeEventListener called:`, event);
            if (obj._eventListeners && obj._eventListeners[event]) {
                const index = obj._eventListeners[event].indexOf(callback);
                if (index > -1) {
                    obj._eventListeners[event].splice(index, 1);
                }
            }
        },
        enumerable: false,
        writable: true,
        configurable: true
    });
    
    Object.defineProperty(obj, 'dispatchEvent', {
        value: function(event) {
            console.log(`${objectName} dispatchEvent called:`, event);
            if (obj._eventListeners && obj._eventListeners[event.type]) {
                obj._eventListeners[event.type].forEach(callback => {
                    try {
                        callback(event);
                    } catch (e) {
                        console.error(`Error in ${objectName} event callback:`, e);
                    }
                });
            }
        },
        enumerable: false,
        writable: true,
        configurable: true
    });
}

function copyStaticProperties(target, source) {
    Object.getOwnPropertyNames(source).forEach(name => {
        if (name !== 'prototype' && name !== 'name' && name !== 'length') {
            try {
                target[name] = source[name];
            } catch (e) {
                // Ignore read-only properties
            }
        }
    });
}

function setupSaferGlobalEventListenerInterceptor() {
    // Intercept common TypeError messages to catch missing addEventListener
    const originalTypeError = window.TypeError;
    window.TypeError = function(message) {
        if (message && message.includes('.addEventListener is not a function')) {
            console.error('=== INTERCEPTED addEventListener TypeError ===');
            console.error('Error message:', message);
            
            // Try to extract the variable name that failed
            const match = message.match(/(\w+)\.addEventListener is not a function/);
            if (match) {
                const varName = match[1];
                console.error('Variable that failed:', varName);
                console.error('This typically means a WebRTC object needs addEventListener polyfill');
            }
        }
        return new originalTypeError(message);
    };
    
    // Add a universal polyfill for common WebRTC objects that might be missing addEventListener
    const originalDefineProperty = Object.defineProperty;
    
    // Intercept property access in a safer way by monitoring specific patterns
    window.catchEventListenerErrors = function() {
        const originalAddEventListener = EventTarget.prototype.addEventListener;
        
        // Override Object.prototype.addEventListener getter for non-EventTarget objects only
        if (!Object.prototype.hasOwnProperty('addEventListener')) {
            Object.defineProperty(Object.prototype, 'addEventListener', {
                get: function() {
                    // Only apply to objects that aren't already EventTargets
                    if (this instanceof EventTarget) {
                        return originalAddEventListener;
                    }
                    
                    // Check if this is likely a WebRTC-related object
                    const constructorName = this.constructor ? this.constructor.name : 'unknown';
                    const isWebRTCRelated = constructorName.includes('RTC') || 
                                          constructorName.includes('Media') || 
                                          constructorName.includes('Track') ||
                                          constructorName.includes('Stream') ||
                                          (typeof this === 'object' && this !== null);
                    
                    if (isWebRTCRelated && !this.hasOwnProperty('addEventListener')) {
                        console.warn(`Adding addEventListener polyfill to ${constructorName} via global getter`);
                        addEventListenerPolyfill(this, constructorName);
                        return this.addEventListener;
                    }
                    
                    return undefined;
                },
                enumerable: false,
                configurable: true
            });
            console.warn('=== ADDED SAFER addEventListener TO Object.prototype ===');
        }
    };
    
    // Apply the safer interceptor
    window.catchEventListenerErrors();
    
    console.log('=== SAFER GLOBAL INTERCEPTOR SETUP COMPLETE ===');
}

function setupGlobalEventListenerInterceptor() {
    try {
        const descriptor = {
            get: function() {
                console.warn('=== GLOBAL addEventListener GETTER CALLED ===');
                console.warn('this object:', this);
                console.warn('this constructor:', this.constructor ? this.constructor.name : 'unknown');
                
                // Prevent infinite recursion by checking if we're already processing this object
                if (this && this._addingEventListener) {
                    console.warn('Preventing infinite recursion - already adding addEventListener to this object');
                    return undefined;
                }
                
                // If this object doesn't have addEventListener, add it
                if (this && !this.hasOwnProperty('addEventListener')) {
                    this._addingEventListener = true;
                    console.warn('Adding addEventListener to object via global getter');
                    addEventListenerPolyfill(this, this.constructor ? this.constructor.name : 'global-getter');
                    delete this._addingEventListener;
                    return this.addEventListener;
                }
                
                // Return undefined if we can't help
                return undefined;
            },
            enumerable: false,
            configurable: true
        };
        
        // Add to Object.prototype as absolute last resort (dangerous but necessary)
        if (!Object.prototype.addEventListener) {
            Object.defineProperty(Object.prototype, 'addEventListener', descriptor);
            console.warn('=== ADDED addEventListener TO Object.prototype AS LAST RESORT ===');
        }
    } catch (e) {
        console.warn('Failed to set up global addEventListener interceptor:', e);
    }
}