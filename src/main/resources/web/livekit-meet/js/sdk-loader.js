let livekitScriptLoaded = false;
let livekitScriptFailed = false;

function getJsVersion() {
    const params = new URLSearchParams(window.location.search);
    return params.get('jsVersion') || '2.6.4';
}

function loadScript(src) {
    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = src;
        script.onload = resolve;
        script.onerror = reject;
        document.body.appendChild(script);
    });
}

function loadLiveKitSdk() {
    const version = getJsVersion();
    const sdkPath = 'lib/livekit-client/v' + version + '/livekit-client.umd.js';

    console.log('Loading LiveKit SDK version:', version);
    console.log('SDK path:', sdkPath);

    const script = document.createElement('script');
    script.src = sdkPath;
    script.onload = function() {
        livekitScriptLoaded = true;
        console.log('LiveKit script onload event fired');
        onSdkLoaded();
    };
    script.onerror = function() {
        livekitScriptFailed = true;
        console.error('LiveKit script onerror event fired for path:', sdkPath);
        onSdkLoadFailed();
    };
    document.head.appendChild(script);
}

async function onSdkLoaded() {
    markStepActive('step-sdk-load');

    console.log('=== LIVEKIT SDK LOADING DETECTION ===');
    console.log('livekitScriptLoaded:', livekitScriptLoaded);
    console.log('livekitScriptFailed:', livekitScriptFailed);
    console.log('typeof LivekitClient:', typeof LivekitClient);

    if (typeof LivekitClient === 'undefined') {
        console.error('LiveKit SDK loaded but LivekitClient is undefined');
        markStepFailed('step-sdk-load', 'SDK loaded but client undefined');
        updateStatus('LiveKit SDK failed to initialize', 'error');
        return;
    }

    console.log('LiveKit SDK loaded successfully, real WebRTC connections will be used');
    progressToNextStep('step-sdk-load', 'Real SDK v' + getJsVersion() + ' loaded');
    addTechnicalDetail('✅ LiveKit SDK v' + getJsVersion() + ' loaded successfully');
    addTechnicalDetail('✅ Real WebRTC connections will be used');
    updateStatus('LiveKit SDK ready', 'success');

    window.LiveKit = LivekitClient;

    try {
        await loadScript('js/livekit-client.js');
        await loadScript('js/test-helpers.js');
        await loadScript('js/app-init.js');
        console.log('All application scripts loaded successfully');
    } catch (error) {
        console.error('Failed to load application scripts:', error);
        updateStatus('Failed to load application scripts', 'error');
    }
}

function onSdkLoadFailed() {
    markStepFailed('step-sdk-load', 'SDK failed to load');
    addTechnicalDetail('❌ LiveKit SDK v' + getJsVersion() + ' failed to load');
    updateStatus('LiveKit SDK failed to load - check version exists', 'error');
}

loadLiveKitSdk();
