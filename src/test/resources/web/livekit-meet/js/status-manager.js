// Status management system for LiveKit Meet UI
class StatusManager {
    constructor() {
        this.statusSteps = [
            'step-sdk-load',
            'step-polyfill', 
            'step-media',
            'step-room',
            'step-connect',
            'step-webrtc',
            'step-complete'
        ];
        
        this.currentStepIndex = 0;
        this.techDetails = [];
    }
    
    updateStatus(message, type = 'info') {
        const statusDiv = document.getElementById('connectionStatus');
        if (statusDiv) {
            statusDiv.textContent = message;
            statusDiv.className = `status ${type}`;
        }
    }
    
    setStepStatus(stepId, status, message = null) {
        const stepElement = document.getElementById(stepId);
        if (stepElement) {
            stepElement.className = `status-step ${status}`;
            if (message) {
                const originalText = stepElement.textContent.split('...')[0];
                stepElement.textContent = originalText + '... ' + message;
            }
            
            // Update emoji based on status
            let emoji = '⏳';
            switch(status) {
                case 'active': emoji = '🔄'; break;
                case 'completed': emoji = '✅'; break;
                case 'failed': emoji = '❌'; break;
                case 'pending': emoji = '⏳'; break;
            }
            stepElement.textContent = emoji + ' ' + stepElement.textContent.substring(2);
        }
    }
    
    markStepCompleted(stepId, message = 'Done') {
        this.setStepStatus(stepId, 'completed', message);
    }
    
    markStepFailed(stepId, message = 'Failed') {
        this.setStepStatus(stepId, 'failed', message);
    }
    
    markStepActive(stepId, message = null) {
        this.setStepStatus(stepId, 'active', message);
    }
    
    addTechnicalDetail(detail) {
        const timestamp = new Date().toLocaleTimeString();
        this.techDetails.push(`[${timestamp}] ${detail}`);
        
        const techInfoDiv = document.getElementById('techInfo');
        if (techInfoDiv) {
            techInfoDiv.textContent = this.techDetails.join('\n');
            techInfoDiv.scrollTop = techInfoDiv.scrollHeight;
        }
    }
    
    progressToNextStep(currentStepId, message = 'Done') {
        this.markStepCompleted(currentStepId, message);
        this.currentStepIndex++;
        if (this.currentStepIndex < this.statusSteps.length) {
            this.markStepActive(this.statusSteps[this.currentStepIndex]);
        }
    }
}

// Global status manager instance
window.statusManager = new StatusManager();

// Global helper functions for backward compatibility
function updateStatus(message, type) { window.statusManager.updateStatus(message, type); }
function setStepStatus(stepId, status, message) { window.statusManager.setStepStatus(stepId, status, message); }
function markStepCompleted(stepId, message) { window.statusManager.markStepCompleted(stepId, message); }
function markStepFailed(stepId, message) { window.statusManager.markStepFailed(stepId, message); }
function markStepActive(stepId, message) { window.statusManager.markStepActive(stepId, message); }
function addTechnicalDetail(detail) { window.statusManager.addTechnicalDetail(detail); }
function progressToNextStep(currentStepId, message) { window.statusManager.progressToNextStep(currentStepId, message); }