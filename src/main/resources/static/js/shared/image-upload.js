/**
 * SIT Campus App — Shared Image Upload Helper
 * Handles drag-and-drop, validation (type + size), FileReader preview.
 *
 * Usage:
 *   const uploader = initImageUpload({
 *       areaId: 'uploadArea', inputId: 'fileInput',
 *       promptId: 'prompt',  previewId: 'preview',
 *       imgId: 'previewImg', removeId: 'removeBtn',
 *       errorId: 'errorEl',
 *       requiredMsg: 'A photo is required.',   // shown on remove (optional)
 *       onSelect: (file) => { ... },           // optional callback
 *       onRemove: () => { ... }                // optional callback
 *   });
 *
 *   uploader.hasFile()   // boolean
 *   uploader.getFile()   // File | null
 *   uploader.reset()     // clear everything
 *   uploader.setError(m) // show error manually
 */
function initImageUpload(cfg) {
    const area    = document.getElementById(cfg.areaId);
    const input   = document.getElementById(cfg.inputId);
    const prompt  = document.getElementById(cfg.promptId);
    const preview = document.getElementById(cfg.previewId);
    const img     = document.getElementById(cfg.imgId);
    const rmBtn   = document.getElementById(cfg.removeId);
    const errEl   = document.getElementById(cfg.errorId);
    const maxMB   = cfg.maxSizeMB || 5;

    if (!area || !input) return null;

    let file = null;

    function clearErr() { errEl.textContent = ''; area.classList.remove('sit-upload-area--error'); }
    function setError(m) { errEl.textContent = m; area.classList.add('sit-upload-area--error'); }

    function handle(f) {
        clearErr();
        if (!f.type.startsWith('image/')) { setError('Only image files are allowed.'); return; }
        if (f.size > maxMB * 1024 * 1024) { setError(`Image must be under ${maxMB} MB.`); return; }
        file = f;
        const reader = new FileReader();
        reader.onload = ev => {
            img.src = ev.target.result;
            prompt.style.display = 'none';
            preview.style.display = 'flex';
        };
        reader.readAsDataURL(f);
        if (cfg.onSelect) cfg.onSelect(f);
    }

    area.addEventListener('click', e => { if (!e.target.closest('#' + cfg.removeId)) input.click(); });
    area.addEventListener('dragover', e => { e.preventDefault(); area.classList.add('sit-upload-area--drag'); });
    area.addEventListener('dragleave', () => area.classList.remove('sit-upload-area--drag'));
    area.addEventListener('drop', e => {
        e.preventDefault(); area.classList.remove('sit-upload-area--drag');
        if (e.dataTransfer.files[0]) handle(e.dataTransfer.files[0]);
    });
    input.addEventListener('change', () => { if (input.files[0]) handle(input.files[0]); });
    rmBtn.addEventListener('click', e => {
        e.stopPropagation(); file = null; input.value = ''; img.src = '';
        preview.style.display = 'none'; prompt.style.display = 'flex';
        if (cfg.requiredMsg) setError(cfg.requiredMsg);
        if (cfg.onRemove) cfg.onRemove();
    });

    return {
        getFile:  () => file,
        hasFile:  () => !!file,
        setError: setError,
        reset() {
            file = null; input.value = ''; img.src = '';
            preview.style.display = 'none'; prompt.style.display = 'flex';
            clearErr();
        }
    };
}
