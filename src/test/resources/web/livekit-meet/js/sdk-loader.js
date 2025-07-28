// LiveKit SDK loading and initialization
let livekitScriptLoaded = false;
let livekitScriptFailed = false;

// Track script loading events
window.addEventListener('load', function() {
    markStepActive('step-sdk-load');
    updateStatus('Checking LiveKit SDK loading...', 'info');
    addTechnicalDetail('Page loaded, checking SDK status');
    
    console.log('=== LIVEKIT SDK LOADING DETECTION ===');
    console.log('livekitScriptLoaded:', livekitScriptLoaded);
    console.log('livekitScriptFailed:', livekitScriptFailed);
    console.log('typeof LivekitClient:', typeof LivekitClient);
    
    addTechnicalDetail(`Script loaded: ${livekitScriptLoaded}`);
    addTechnicalDetail(`Script failed: ${livekitScriptFailed}`);
    addTechnicalDetail(`LivekitClient type: ${typeof LivekitClient}`);
    
    // Chrome provides fake media devices via --use-fake-device-for-media-stream
    // No polyfills needed - trust Chrome's built-in fake media handling
    
    // Check if LiveKit SDK loaded successfully
    if (!livekitScriptLoaded || livekitScriptFailed || typeof LivekitClient === 'undefined') {
        console.error('LiveKit SDK not loaded properly!');
        console.error('Reasons: scriptLoaded=' + livekitScriptLoaded + ', scriptFailed=' + livekitScriptFailed + ', LivekitClient=' + (typeof LivekitClient));
        
        markStepFailed('step-sdk-load', 'SDK failed to load');
        addTechnicalDetail('❌ LiveKit SDK failed to load properly');
        updateStatus('LiveKit SDK failed to load - cannot proceed', 'error');
        
        // Stop execution - we cannot proceed without the real SDK
        throw new Error('LiveKit SDK is required but failed to load');
    } else {
        console.log('LiveKit SDK loaded successfully, real WebRTC connections will be used');
        progressToNextStep('step-sdk-load', 'Real SDK loaded');
        addTechnicalDetail('✅ LiveKit SDK loaded successfully');
        addTechnicalDetail('✅ Real WebRTC connections will be used');
        updateStatus('LiveKit SDK ready', 'success');
        
        // Map LivekitClient to LiveKit for backward compatibility
        window.LiveKit = LivekitClient;
    }
});

// Script loading event handlers
function onLiveKitScriptLoad() {
    livekitScriptLoaded = true;
    console.log('LiveKit script onload event fired');
}

function onLiveKitScriptError() {
    livekitScriptFailed = true;
    console.error('LiveKit script onerror event fired');
}