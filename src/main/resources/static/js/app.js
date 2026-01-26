/**
 * Kade Test Gen - Frontend Application
 */

// Theme Management
function initTheme() {
    const saved = localStorage.getItem('theme');
    // Default to light theme
    const isDark = saved ? saved === 'dark' : false;
    setTheme(isDark ? 'dark' : 'light');
}

function setTheme(theme) {
    const isDark = theme === 'dark';
    document.documentElement.classList.toggle('light-theme', !isDark);
    localStorage.setItem('theme', theme);
    const toggle = document.getElementById('themeToggle');
    if (toggle) {
        const icon = toggle.querySelector('i');
        if (icon) {
            icon.className = isDark ? 'bi bi-moon-stars' : 'bi bi-sun';
        }
    }
}

// State management
const state = {
    inputSource: 'document', // 'document' | 'github' | 'text'
    testType: 'UI',
    selectedFile: null,
    isGenerating: false,
    results: null,
    // New control states
    testCategory: 'smoke',
    coverageLevel: 3,
    assertionStrictness: 'medium',
    waitStrategy: 'explicit',
    generateNegative: true,
    addAssertions: true,
    handleDynamic: true,
    generateUtilities: true,
    // Copilot/AI state
    useCopilot: false,
    copilotEnabled: false
};

// DOM Elements
const elements = {
    sourceTabs: document.querySelectorAll('.source-tab'),
    sourceContents: document.querySelectorAll('.source-content'),
    testTypeOptions: document.querySelectorAll('.test-type-option'),
    fileUpload: document.getElementById('fileUpload'),
    fileInput: document.getElementById('fileInput'),
    fileName: document.getElementById('fileName'),
    generateBtn: document.getElementById('generateBtn'),
    resultsSection: document.getElementById('resultsSection'),
    resultsContainer: document.getElementById('resultsContainer'),
    resultsMeta: document.getElementById('resultsMeta'),
    configToggle: document.getElementById('configToggle'),
    configSection: document.getElementById('configSection')
};

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initThemeToggle();
    initModeTabs();        // New mode tab switching
    initSourceSelector();  // New source button selector
    initTestTypeSelector(); // New test type selector
    initFileUpload();
    initConfigToggle();
    initGenerateButton();
    initToggleOptions();
    initResultsNavLink();
    initCopilotToggle();
    initUrlAnalysis();
});

// Theme Toggle
function initThemeToggle() {
    const toggle = document.getElementById('themeToggle');
    if (toggle) {
        toggle.addEventListener('click', () => {
            const current = localStorage.getItem('theme') || 'dark';
            setTheme(current === 'dark' ? 'light' : 'dark');
        });
    }
}

// Mode Tab Switching (Analyze vs Generate)
function initModeTabs() {
    const modeTabs = document.querySelectorAll('.mode-tab');
    const modeContents = document.querySelectorAll('.mode-content');

    modeTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const mode = tab.dataset.mode;

            // Update tab styles
            modeTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            // Show/hide content
            modeContents.forEach(content => {
                if (content.dataset.mode === mode) {
                    content.classList.add('active');
                    content.style.display = 'block';
                } else {
                    content.classList.remove('active');
                    content.style.display = 'none';
                }
            });
        });
    });
}

// Source Selector (Document, GitHub, Text) for Generate mode
function initSourceSelector() {
    const sourceButtons = document.querySelectorAll('.source-btn');
    const sourceContents = document.querySelectorAll('.mode-content[data-mode="generate"] .source-content');

    sourceButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const source = btn.dataset.source;
            state.inputSource = source;

            // Update button styles
            sourceButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            // Show/hide content
            sourceContents.forEach(content => {
                if (content.dataset.source === source) {
                    content.classList.add('active');
                    content.style.display = 'block';
                } else {
                    content.classList.remove('active');
                    content.style.display = 'none';
                }
            });
        });
    });
}

// Test Type Selector (UI, API, Mobile)
function initTestTypeSelector() {
    const typeButtons = document.querySelectorAll('.type-btn');

    typeButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const type = btn.dataset.type;
            state.testType = type;

            // Update button styles
            typeButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            // Update URL label based on test type
            updateTargetUrlLabel(type);
        });
    });
}

function updateTargetUrlLabel(testType) {
    const targetUrlGroup = document.getElementById('targetUrlGroup');
    const targetUrlLabel = document.getElementById('targetUrlLabel');
    const applicationUrl = document.getElementById('applicationUrl');
    const targetUrlHint = document.getElementById('targetUrlHint');

    if (!targetUrlGroup || !targetUrlLabel || !applicationUrl) return;

    if (testType === 'UI') {
        targetUrlGroup.style.display = 'block';
        targetUrlLabel.innerHTML = 'Target Environment <span class="optional">(optional)</span>';
        applicationUrl.placeholder = 'https://example.com';
        if (targetUrlHint) targetUrlHint.textContent = 'Auto-detected from input if not specified';
    } else if (testType === 'API') {
        targetUrlGroup.style.display = 'block';
        targetUrlLabel.innerHTML = 'API Base URL <span class="optional">(optional)</span>';
        applicationUrl.placeholder = 'https://api.example.com';
        if (targetUrlHint) targetUrlHint.textContent = 'Extracted from requirements if not specified';
    } else if (testType === 'MOBILE') {
        // For mobile, hide the URL field - Appium server is typically configured elsewhere
        targetUrlGroup.style.display = 'none';
    }
}

/**
 * Extract URLs from text content.
 * Returns the first http/https URL found, or null if none.
 */
function extractUrlFromText(text) {
    if (!text) return null;
    const urlPattern = /https?:\/\/[^\s<>"')\]]+/gi;
    const matches = text.match(urlPattern);
    if (matches && matches.length > 0) {
        // Return the first URL, cleaned up (remove trailing punctuation)
        return matches[0].replace(/[.,;:!?)]+$/, '');
    }
    return null;
}

/**
 * Get the application URL - either from user input or extracted from content.
 */
function getApplicationUrl() {
    const explicitUrl = document.getElementById('applicationUrl')?.value?.trim();

    // If user provided a URL, use it
    if (explicitUrl) {
        return explicitUrl;
    }

    // Try to extract from the input content based on source type
    let content = '';
    if (state.inputSource === 'text') {
        content = document.getElementById('textContent')?.value || '';
    }
    // Note: For document/github, we can't easily extract here,
    // so let the backend handle URL extraction from parsed content

    const extractedUrl = extractUrlFromText(content);
    return extractedUrl || ''; // Empty string means backend will use default
}

// File Upload
function initFileUpload() {
    const fileUpload = document.getElementById('fileUpload') || document.querySelector('.file-upload-zone');
    const fileInput = document.getElementById('fileInput');
    const fileName = document.getElementById('fileName');

    if (!fileUpload || !fileInput) return;

    // Click to upload
    fileUpload.addEventListener('click', () => fileInput.click());

    // Drag and drop
    fileUpload.addEventListener('dragover', (e) => {
        e.preventDefault();
        fileUpload.classList.add('dragover');
    });

    fileUpload.addEventListener('dragleave', () => {
        fileUpload.classList.remove('dragover');
    });

    fileUpload.addEventListener('drop', (e) => {
        e.preventDefault();
        fileUpload.classList.remove('dragover');

        const files = e.dataTransfer.files;
        if (files.length > 0) {
            handleFileSelect(files[0]);
        }
    });

    // File input change
    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleFileSelect(e.target.files[0]);
        }
    });
}

function handleFileSelect(file) {
    const fileName = file.name.toLowerCase();
    const validExtensions = ['.docx', '.doc', '.txt'];
    const isValid = validExtensions.some(ext => fileName.endsWith(ext));

    if (!isValid) {
        showToast('Please upload a .docx, .doc, or .txt file', 'error');
        return;
    }

    state.selectedFile = file;
    const fileNameEl = document.getElementById('fileName');
    if (fileNameEl) {
        fileNameEl.textContent = `📄 ${file.name}`;
        fileNameEl.style.display = 'block';
    }
}

// Configuration Toggle
function initConfigToggle() {
    elements.configToggle?.addEventListener('click', () => {
        elements.configSection.classList.toggle('expanded');
    });
}

// Generate Button
function initGenerateButton() {
    elements.generateBtn.addEventListener('click', generateTests);
}

async function generateTests() {
    if (state.isGenerating) return;

    // Validate input
    if (state.inputSource === 'document' && !state.selectedFile) {
        showToast('Please upload a Word document', 'error');
        return;
    }

    if (state.inputSource === 'github') {
        const repoUrlEl = document.getElementById('repoUrl');
        if (!repoUrlEl || !repoUrlEl.value.trim()) {
            showToast('Please enter a GitHub repository URL', 'error');
            return;
        }
    }

    if (state.inputSource === 'text') {
        const textContentEl = document.getElementById('textContent');
        if (!textContentEl || !textContentEl.value.trim()) {
            showToast('Please enter requirements content', 'error');
            return;
        }
    }

    // Start generation
    state.isGenerating = true;
    updateGenerateButton(true);

    try {
        let response;

        if (state.inputSource === 'document') {
            response = await generateFromDocument();
        } else if (state.inputSource === 'github') {
            response = await generateFromGitHub();
        } else {
            response = await generateFromText();
        }

        if (response.success) {
            state.results = response;
            displayResults(response);
            showToast(`Generated ${response.totalTests} test artifacts!`, 'success');
        } else {
            showToast(response.message || 'Generation failed', 'error');
        }
    } catch (error) {
        console.error('Generation error:', error);
        showToast('Error generating tests: ' + error.message, 'error');
    } finally {
        state.isGenerating = false;
        updateGenerateButton(false);
    }
}

async function generateFromDocument() {
    const formData = new FormData();
    formData.append('file', state.selectedFile);
    formData.append('testType', state.testType);
    formData.append('basePackage', document.getElementById('basePackage')?.value || 'com.enterprise.tests');
    formData.append('applicationUrl', getApplicationUrl());
    formData.append('apiBaseUrl', getApplicationUrl()); // Use same URL for API base
    formData.append('appiumServerUrl', document.getElementById('appiumServerUrl')?.value || '');
    formData.append('appPackage', document.getElementById('appPackage')?.value || '');
    formData.append('bundleId', document.getElementById('bundleId')?.value || '');
    // Control panel options
    formData.append('testCategory', state.testCategory);
    formData.append('coverageLevel', state.coverageLevel);
    formData.append('assertionStrictness', state.assertionStrictness);
    formData.append('waitStrategy', state.waitStrategy);
    formData.append('generateNegative', state.generateNegative);
    formData.append('addAssertions', state.addAssertions);
    formData.append('handleDynamic', state.handleDynamic);
    formData.append('generateUtilities', state.generateUtilities);

    const response = await fetch('/api/generate/document', {
        method: 'POST',
        body: formData
    });

    return response.json();
}

async function generateFromGitHub() {
    const formData = new FormData();
    formData.append('repositoryUrl', document.getElementById('repoUrl')?.value || '');
    formData.append('githubToken', document.getElementById('githubToken')?.value || '');
    formData.append('testType', state.testType);
    formData.append('basePackage', document.getElementById('basePackage')?.value || 'com.enterprise.tests');
    formData.append('applicationUrl', getApplicationUrl());
    formData.append('apiBaseUrl', getApplicationUrl()); // Use same URL for API base
    formData.append('appiumServerUrl', document.getElementById('appiumServerUrl')?.value || '');
    formData.append('appPackage', document.getElementById('appPackage')?.value || '');
    formData.append('bundleId', document.getElementById('bundleId')?.value || '');
    // Control panel options
    formData.append('testCategory', state.testCategory);
    formData.append('coverageLevel', state.coverageLevel);
    formData.append('assertionStrictness', state.assertionStrictness);
    formData.append('waitStrategy', state.waitStrategy);
    formData.append('generateNegative', state.generateNegative);
    formData.append('addAssertions', state.addAssertions);
    formData.append('handleDynamic', state.handleDynamic);
    formData.append('generateUtilities', state.generateUtilities);

    const response = await fetch('/api/generate/github', {
        method: 'POST',
        body: formData
    });

    return response.json();
}

async function generateFromText() {
    const content = document.getElementById('textContent')?.value || '';
    const appUrl = getApplicationUrl();

    const payload = {
        content: content,
        testType: state.testType,
        basePackage: document.getElementById('basePackage')?.value || 'com.enterprise.tests',
        applicationUrl: appUrl,
        apiBaseUrl: appUrl, // Use same URL for API base
        appiumServerUrl: document.getElementById('appiumServerUrl')?.value || '',
        appPackage: document.getElementById('appPackage')?.value || '',
        bundleId: document.getElementById('bundleId')?.value || '',
        // Control panel options
        testCategory: state.testCategory,
        coverageLevel: state.coverageLevel,
        assertionStrictness: state.assertionStrictness,
        waitStrategy: state.waitStrategy,
        generateNegative: state.generateNegative,
        addAssertions: state.addAssertions,
        handleDynamic: state.handleDynamic,
        generateUtilities: state.generateUtilities
    };

    const response = await fetch('/api/generate/text', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    });

    return response.json();
}

function updateGenerateButton(loading) {
    const btn = elements.generateBtn;
    if (loading) {
        btn.innerHTML = '<div class="spinner"></div> Generating...';
        btn.disabled = true;
    } else {
        btn.innerHTML = 'Generate Tests';
        btn.disabled = false;
    }
}

// Display Results
function displayResults(response) {
    elements.resultsSection.classList.add('visible');

    // Update meta
    elements.resultsMeta.innerHTML = `
        <span>📊 ${response.totalTests} test artifacts</span>
        <span>⏱️ ${response.generationTimeMs}ms</span>
        <span>🔧 ${response.frameworkInfo}</span>
    `;

    // Display warnings if any
    let warningsHtml = '';
    if (response.warnings && response.warnings.length > 0) {
        warningsHtml = `
            <div class="gaps-list" style="margin-bottom: 1rem;">
                ${response.warnings.map(w => `<div class="gap-item">${escapeHtml(w)}</div>`).join('')}
            </div>
        `;
    }

    // Display test cases
    const testCasesHtml = response.testCases.map((tc, index) => createTestCaseCard(tc, index)).join('');

    elements.resultsContainer.innerHTML = warningsHtml + testCasesHtml;

    // Add click handlers for expand/collapse
    document.querySelectorAll('.test-case-header').forEach(header => {
        header.addEventListener('click', () => {
            header.parentElement.classList.toggle('expanded');
        });
    });

    // Add copy handlers
    document.querySelectorAll('.code-copy').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const code = btn.closest('.code-block').querySelector('pre').textContent;
            navigator.clipboard.writeText(code).then(() => {
                btn.textContent = '✓ Copied!';
                setTimeout(() => {
                    btn.textContent = '📋 Copy';
                }, 2000);
            });
        });
    });

    // Add download handlers
    document.querySelectorAll('.code-download').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const code = btn.closest('.code-section').querySelector('pre').textContent;
            const filename = btn.dataset.filename + '.java';
            downloadFile(code, filename);
        });
    });

    // Apply syntax highlighting
    document.querySelectorAll('pre').forEach(block => {
        hljs.highlightElement(block);
    });

    // Scroll to results
    elements.resultsSection.scrollIntoView({ behavior: 'smooth' });
}

function createTestCaseCard(testCase, index) {
    const isBaseClass = testCase.testId === 'TC-000';

    return `
        <div class="test-case-card ${index === 0 ? 'expanded' : ''}">
            <div class="test-case-header">
                <div class="test-case-title">
                    <span class="test-case-id">${testCase.testId}</span>
                    <span class="test-case-name">${escapeHtml(testCase.testName)}</span>
                </div>
                <span class="test-case-type">${testCase.testType}</span>
            </div>
            <div class="test-case-body">
                ${!isBaseClass ? `
                    <div class="test-case-section">
                        <div class="test-case-section-title">Scenario Description</div>
                        <p>${escapeHtml(testCase.scenarioDescription)}</p>
                    </div>

                    <div class="test-case-section">
                        <div class="test-case-section-title">Preconditions</div>
                        <ul>
                            ${testCase.preconditions.map(p => `<li>${escapeHtml(p)}</li>`).join('')}
                        </ul>
                    </div>

                    ${testCase.testSteps.length > 0 ? `
                        <div class="test-case-section">
                            <div class="test-case-section-title">Test Steps</div>
                            <ul>
                                ${testCase.testSteps.map(s => `
                                    <li><strong>Step ${s.stepNumber}:</strong> ${escapeHtml(s.action)}
                                        ${s.testData ? `<br><em>Data: ${escapeHtml(s.testData)}</em>` : ''}
                                    </li>
                                `).join('')}
                            </ul>
                        </div>
                    ` : ''}

                    <div class="test-case-section">
                        <div class="test-case-section-title">Expected Results</div>
                        <ul>
                            ${testCase.expectedResults.map(r => `<li>${escapeHtml(r)}</li>`).join('')}
                        </ul>
                    </div>

                    ${testCase.identifiedGaps && testCase.identifiedGaps.length > 0 ? `
                        <div class="test-case-section">
                            <div class="test-case-section-title">Identified Gaps</div>
                            <div class="gaps-list">
                                ${testCase.identifiedGaps.map(g => `<div class="gap-item">${escapeHtml(g)}</div>`).join('')}
                            </div>
                        </div>
                    ` : ''}
                ` : ''}

                <div class="test-case-section code-section">
                    <div class="code-header-top">
                        <div class="code-label">Java Automation Code</div>
                        <div class="code-actions">
                            <button class="code-btn code-copy" title="Copy code to clipboard"><i class="bi bi-files"></i><span>Copy</span></button>
                            <button class="code-btn code-download" data-filename="${testCase.className}" title="Download as .java file"><i class="bi bi-download"></i><span>Download</span></button>
                        </div>
                    </div>
                    <div class="code-block">
                        <div class="code-header">
                            <span class="code-language">Java • ${testCase.className}.java</span>
                        </div>
                        <div class="code-content">
                            <pre><code class="language-java">${escapeHtml(testCase.automationCode)}</code></pre>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

// Toast Notifications
function showToast(message, type = 'info') {
    // Remove existing toasts
    document.querySelectorAll('.toast').forEach(t => t.remove());

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <span>${type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️'}</span>
        <span>${escapeHtml(message)}</span>
    `;

    document.body.appendChild(toast);

    // Trigger animation
    requestAnimationFrame(() => {
        toast.classList.add('visible');
    });

    // Auto-remove
    setTimeout(() => {
        toast.classList.remove('visible');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Utility Functions
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function downloadFile(content, filename) {
    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(content));
    element.setAttribute('download', filename);
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
}

// ==========================================
// TOGGLE OPTIONS - Simplified Controls
// ==========================================

// Toggle Options
function initToggleOptions() {
    const toggleMap = {
        'toggleNegative': 'generateNegative',
        'toggleAssertions': 'addAssertions',
        'toggleDynamic': 'handleDynamic',
        'toggleUtilities': 'generateUtilities'
    };

    Object.entries(toggleMap).forEach(([elementId, stateKey]) => {
        const checkbox = document.getElementById(elementId);
        if (checkbox) {
            checkbox.addEventListener('change', () => {
                state[stateKey] = checkbox.checked;
            });
        }
    });
}

// Results Nav Link - scroll to results section
function initResultsNavLink() {
    const resultsLink = document.getElementById('resultsNavLink');
    if (resultsLink) {
        resultsLink.addEventListener('click', (e) => {
            e.preventDefault();
            const resultsSection = document.getElementById('resultsSection');
            if (resultsSection) {
                resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    }
}

// Copilot Toggle - Check status and initialize
async function initCopilotToggle() {
    const toggle = document.getElementById('toggleCopilot');
    const badge = document.getElementById('copilotBadge');

    if (!toggle || !badge) return;

    // Check Copilot status from API
    try {
        const response = await fetch('/api/copilot/status');
        const status = await response.json();

        state.copilotEnabled = status.enabled;

        if (status.enabled) {
            badge.textContent = 'Ready';
            badge.classList.add('enabled');
            toggle.disabled = false;
        } else {
            badge.textContent = 'Configure';
            badge.classList.remove('enabled');
            toggle.disabled = false; // Still allow toggling, but show warning
        }
    } catch (e) {
        badge.textContent = 'Offline';
        toggle.disabled = true;
    }

    // Handle toggle change
    toggle.addEventListener('change', () => {
        state.useCopilot = toggle.checked;
        if (toggle.checked && !state.copilotEnabled) {
            showToast('Copilot not configured. Set AZURE_OPENAI_ENDPOINT and AZURE_OPENAI_API_KEY environment variables.', 'error');
            toggle.checked = false;
            state.useCopilot = false;
        } else if (toggle.checked) {
            showToast('AI Copilot enabled - Enhanced test generation active', 'success');
        }
    });
}

// ==========================================
// URL ANALYSIS - Live Page DOM Analysis
// ==========================================

function initUrlAnalysis() {
    const analyzeBtn = document.getElementById('analyzeUrlBtn');
    const urlInput = document.getElementById('liveUrlInput');
    const copyBddBtn = document.getElementById('copyBddBtn');
    const downloadBddBtn = document.getElementById('downloadBddBtn');

    if (analyzeBtn && urlInput) {
        analyzeBtn.addEventListener('click', () => analyzeUrl());

        // Also allow Enter key to trigger analysis
        urlInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                analyzeUrl();
            }
        });
    }

    if (copyBddBtn) {
        copyBddBtn.addEventListener('click', () => {
            const bddOutput = document.getElementById('bddOutput');
            const bddContent = bddOutput.dataset.rawContent || bddOutput.textContent;
            navigator.clipboard.writeText(bddContent).then(() => {
                // Visual feedback - change icon temporarily
                const icon = copyBddBtn.querySelector('i');
                const originalClass = icon.className;
                icon.className = 'bi bi-check-lg';
                copyBddBtn.classList.add('copied');
                showToast('BDD scenarios copied to clipboard!', 'success');
                setTimeout(() => {
                    icon.className = originalClass;
                    copyBddBtn.classList.remove('copied');
                }, 2000);
            }).catch(() => {
                showToast('Failed to copy to clipboard', 'error');
            });
        });
    }

    if (downloadBddBtn) {
        downloadBddBtn.addEventListener('click', () => {
            const bddOutput = document.getElementById('bddOutput');
            const bddContent = bddOutput.dataset.rawContent || bddOutput.textContent;
            const pagePurpose = document.getElementById('pagePurpose').textContent || 'page';
            const filename = `${pagePurpose.toLowerCase().replace(/\s+/g, '_')}_tests.feature`;
            downloadFile(bddContent, filename);
            showToast(`Downloaded ${filename}`, 'success');
        });
    }
}

// Progress messages for analysis stages
const analysisProgressMessages = [
    { message: 'Connecting to URL...', icon: 'bi-globe' },
    { message: 'Loading page content...', icon: 'bi-file-earmark-code' },
    { message: 'Waiting for page to stabilize...', icon: 'bi-hourglass-split' },
    { message: 'Extracting form elements...', icon: 'bi-input-cursor-text' },
    { message: 'Analyzing buttons and inputs...', icon: 'bi-ui-checks' },
    { message: 'Detecting navigation links...', icon: 'bi-link-45deg' },
    { message: 'Inferring page purpose...', icon: 'bi-bullseye' },
    { message: 'Identifying user flows...', icon: 'bi-diagram-3' },
    { message: 'Generating BDD scenarios...', icon: 'bi-file-earmark-text' },
    { message: 'Finalizing analysis...', icon: 'bi-check2-circle' }
];

let progressInterval = null;

function startProgressAnimation() {
    const progressContent = document.querySelector('#analysisProgress .progress-content span');
    const progressIcon = document.querySelector('#analysisProgress .progress-content .spinner');

    if (!progressContent) return;

    let currentIndex = 0;

    // Update message immediately
    progressContent.textContent = analysisProgressMessages[0].message;

    // Cycle through messages
    progressInterval = setInterval(() => {
        currentIndex = (currentIndex + 1) % analysisProgressMessages.length;
        const stage = analysisProgressMessages[currentIndex];
        progressContent.textContent = stage.message;
    }, 1500); // Change message every 1.5 seconds
}

function stopProgressAnimation() {
    if (progressInterval) {
        clearInterval(progressInterval);
        progressInterval = null;
    }
}

async function analyzeUrl() {
    const urlInput = document.getElementById('liveUrlInput');
    const progressDiv = document.getElementById('analysisProgress');
    const resultsDiv = document.getElementById('analysisResults');

    let url = urlInput.value.trim();
    if (!url) {
        showToast('Please enter a URL to analyze', 'error');
        return;
    }

    // Add protocol if missing
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
        url = 'https://' + url;
    }

    // Show progress, hide results
    progressDiv.style.display = 'block';
    resultsDiv.style.display = 'none';

    // Start animated progress messages
    startProgressAnimation();

    try {
        const response = await fetch('/api/analyze/url', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ url: url })
        });

        const data = await response.json();

        // Stop progress animation
        stopProgressAnimation();

        if (data.success) {
            displayUrlAnalysisResults(data);
            showToast(`Analysis complete: ${data.scenarioCount} BDD scenarios generated!`, 'success');
        } else {
            // Still display results even on error, so user can see what went wrong
            if (data.bddScenarios) {
                displayUrlAnalysisResults(data);
                showToast(data.error || 'Analysis encountered issues', 'error');
            } else {
                showToast(data.error || 'Analysis failed', 'error');
                progressDiv.style.display = 'none';
            }
        }
    } catch (error) {
        console.error('Analysis error:', error);
        stopProgressAnimation();
        showToast('Error analyzing URL: ' + error.message, 'error');
        progressDiv.style.display = 'none';
    }
}

function displayUrlAnalysisResults(data) {
    const progressDiv = document.getElementById('analysisProgress');
    const resultsDiv = document.getElementById('analysisResults');

    // Hide progress, show results
    progressDiv.style.display = 'none';
    resultsDiv.style.display = 'block';

    // Populate stats bar
    document.getElementById('pagePurpose').textContent = capitalizeFirst(data.analysis.pagePurpose);
    document.getElementById('elementsCount').textContent = data.analysis.totalElements;
    document.getElementById('formsCount').textContent = data.analysis.totalForms;
    document.getElementById('analysisTime').textContent = `${data.analysis.analysisTimeMs}ms`;

    // Populate inferred flows
    const flowsList = document.getElementById('inferredFlows');
    if (flowsList && data.inferredFlows) {
        flowsList.innerHTML = data.inferredFlows.map(flow => `<li>${escapeHtml(flow)}</li>`).join('');
    }

    // Populate edge cases
    const edgeCasesList = document.getElementById('edgeCases');
    if (edgeCasesList && data.edgeCases) {
        edgeCasesList.innerHTML = data.edgeCases.slice(0, 8).map(ec => `<li>${escapeHtml(ec)}</li>`).join('');
    }

    // Populate security considerations
    const securityList = document.getElementById('securityConsiderations');
    if (securityList && data.securityConsiderations) {
        securityList.innerHTML = data.securityConsiderations.slice(0, 6).map(sc => `<li>${escapeHtml(sc)}</li>`).join('');
    }

    // Populate BDD scenarios
    const scenarioCount = document.getElementById('scenarioCount');
    if (scenarioCount) {
        scenarioCount.textContent = data.scenarioCount;
    }

    const bddOutput = document.getElementById('bddOutput');
    if (bddOutput && data.bddScenarios) {
        // Store raw text for copy/download, apply highlighting for display
        bddOutput.dataset.rawContent = data.bddScenarios;
        highlightGherkin(bddOutput, data.bddScenarios);
    }

    // Scroll to results
    resultsDiv.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function highlightGherkin(element, rawText) {
    // Prevent highlight.js from auto-highlighting this element
    element.classList.remove('language-gherkin');
    element.classList.add('gherkin-highlighted');

    // First escape HTML to prevent XSS, then apply highlighting
    let html = escapeHtml(rawText);

    // Highlight keywords - using word boundaries for better matching
    // Order matters: longer keywords first to avoid partial matches
    const keywords = [
        'Scenario Outline:', 'Feature:', 'Background:', 'Scenario:',
        'Examples:', 'Given ', 'When ', 'Then ', 'And ', 'But '
    ];

    keywords.forEach(keyword => {
        const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        // Match keyword at start of line or after newline, with optional leading whitespace
        const regex = new RegExp(`(^|\\n)([ \\t]*)(${escapedKeyword})`, 'gm');
        html = html.replace(regex, '$1$2<span class="gherkin-keyword">$3</span>');
    });

    // Highlight tags (@tag)
    html = html.replace(/(@[\w-]+)/g, '<span class="gherkin-tag">$1</span>');

    // Highlight strings in quotes (escaped quotes from escapeHtml)
    html = html.replace(/&quot;([^&]*)&quot;/g, '&quot;<span class="gherkin-string">$1</span>&quot;');

    // Highlight angle brackets for placeholders like <value>
    html = html.replace(/&lt;(\w+)&gt;/g, '<span class="gherkin-placeholder">&lt;$1&gt;</span>');

    // Highlight table pipes
    html = html.replace(/\|/g, '<span class="gherkin-pipe">|</span>');

    // Highlight comments
    html = html.replace(/(#.*)$/gm, '<span class="gherkin-comment">$1</span>');

    element.innerHTML = html;
}

function capitalizeFirst(str) {
    if (!str) return str;
    return str.charAt(0).toUpperCase() + str.slice(1);
}

// Export functions for use in HTML
window.generateTests = generateTests;
window.analyzeUrl = analyzeUrl;
