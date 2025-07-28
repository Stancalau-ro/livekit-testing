// Application initialization
let liveKitClient;

// Capture console logs for debugging
window.consoleLogCapture = [];
const originalConsoleLog = console.log;
const originalConsoleError = console.error;
const originalConsoleWarn = console.warn;

console.log = function(...args) {
    window.consoleLogCapture.push('LOG: ' + args.join(' '));
    originalConsoleLog.apply(console, args);
};

console.error = function(...args) {
    window.consoleLogCapture.push('ERROR: ' + args.join(' '));
    originalConsoleError.apply(console, args);
};

console.warn = function(...args) {
    window.consoleLogCapture.push('WARN: ' + args.join(' '));
    originalConsoleWarn.apply(console, args);
};

// Capture JavaScript errors for test automation
window.addEventListener('error', function(event) {
    console.error('JavaScript error:', event.error);
    window.lastError = event.error ? event.error.message : event.message;
});

window.addEventListener('unhandledrejection', function(event) {
    console.error('Unhandled promise rejection:', event.reason);
    window.lastError = event.reason ? event.reason.message || event.reason.toString() : 'Promise rejection';
});

// Initialize when page loads
window.addEventListener('load', () => {
    // Update initial status
    document.getElementById('status').textContent = 'Page loaded - starting initialization...';
    setStepStatus('auto-step-params', 'active', 'Checking...');
    
    const urlParams = new URLSearchParams(window.location.search);
    
    // Show URL parameters for debugging
    const allParams = [];
    for (const [key, value] of urlParams.entries()) {
        allParams.push(`${key}=${value}`);
    }
    addTechnicalDetail(`URL Parameters: ${allParams.join(', ') || 'none'}`);
    
    // Populate form fields from URL parameters
    const fields = [
        { param: 'liveKitUrl', element: document.getElementById('liveKitUrl'), default: 'ws://localhost:7880' },
        { param: 'token', element: document.getElementById('token'), default: 'test' },
        { param: 'roomName', element: document.getElementById('roomName'), default: 'TestRoom' },
        { param: 'participantName', element: document.getElementById('participantName'), default: 'Test User' }
    ];
    
    markStepCompleted('auto-step-params', 'URL checked');
    setStepStatus('auto-step-fields', 'active', 'Populating...');
    
    fields.forEach(field => {
        const paramValue = urlParams.get(field.param);
        if (paramValue) {
            field.element.value = paramValue;
            addTechnicalDetail(`Set ${field.param}: ${paramValue}`);
        } else if (!field.element.value) {
            field.element.value = field.default;
            addTechnicalDetail(`Default ${field.param}: ${field.default}`);
        }
    });
    
    markStepCompleted('auto-step-fields', 'Fields populated');
    
    // Initialize LiveKit client
    document.getElementById('status').textContent = 'Initializing LiveKit client...';
    addTechnicalDetail('Creating LiveKitMeetClient instance...');
    liveKitClient = new LiveKitMeetClient();
    
    // Expose client for test automation
    window.liveKitClient = liveKitClient;
    
    // Auto-join if autoJoin parameter is present
    const autoJoinParam = urlParams.get('autoJoin');
    addTechnicalDetail(`AutoJoin parameter: ${autoJoinParam}`);
    
    if (autoJoinParam === 'true') {
        setStepStatus('auto-step-submit', 'active', 'Will auto-submit in 2 seconds...');
        document.getElementById('status').textContent = 'Auto-join enabled - will submit form in 2 seconds...';
        
        let countdown = 2;
        const countdownInterval = setInterval(() => {
            countdown--;
            setStepStatus('auto-step-submit', 'active', `Will auto-submit in ${countdown} seconds...`);
            document.getElementById('status').textContent = `Auto-join in ${countdown} seconds...`;
            
            if (countdown <= 0) {
                clearInterval(countdownInterval);
                markStepCompleted('auto-step-submit', 'Form submitted');
                document.getElementById('status').textContent = 'Auto-submitting form now...';
                addTechnicalDetail('🚀 AUTO-SUBMITTING FORM NOW');
                
                // Hide auto-join status and show connection status
                document.getElementById('autoJoinStatus').style.display = 'none';
                
                document.getElementById('meetingForm').dispatchEvent(new Event('submit', { bubbles: true }));
            }
        }, 1000);
    } else {
        setStepStatus('auto-step-submit', 'pending', 'Auto-join disabled - manual submission required');
        document.getElementById('status').textContent = 'Ready - please click Join Meeting button';
    }
});