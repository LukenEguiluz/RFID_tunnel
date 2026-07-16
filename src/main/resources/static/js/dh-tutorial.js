import { driver } from 'https://cdn.jsdelivr.net/npm/driver.js@1.3.1/+esm';

function normPath() {
    const ctx = document.body && document.body.dataset && document.body.dataset.dhTutorialPage;
    if (ctx === 'error') return '__error__';
    let p = window.location.pathname || '/';
    if (p.length > 1 && p.endsWith('/')) p = p.slice(0, -1);
    return p || '/';
}

function matchRoute(p) {
    if (p === '__error__') return 'errorPage';
    if (p === '/' || p === '') return 'home';
    if (p === '/readers') return 'readers';
    if (p === '/readers/new') return 'readerNew';
    if (/^\/readers\/[^/]+\/edit$/.test(p)) return 'readerEdit';
    if (/^\/readers\/[^/]+\/antennas$/.test(p)) return 'antennas';
    if (p === '/tags') return 'tags';
    if (p === '/groups') return 'groups';
    if (p === '/groups/new') return 'groupNew';
    if (/^\/groups\/[^/]+\/edit$/.test(p)) return 'groupEdit';
    if (p === '/inventory-systems') return 'invList';
    if (p === '/inventory-systems/new') return 'invNew';
    if (/^\/inventory-systems\/[^/]+\/edit$/.test(p)) return 'invEdit';
    if (/^\/inventory-systems\/[^/]+\/epcs$/.test(p)) return 'invEpcs';
    if (p === '/api-docs') return 'apiDocs';
    return 'generic';
}

function pop(el, title, description, side = 'bottom', align = 'start') {
    const step = { popover: { title, description, side, align } };
    if (el) step.element = el;
    return step;
}

function commonSteps() {
    return [
        pop(null, 'Guía de la pantalla', 'Le resaltamos cada zona; el resto queda <strong>oscurecido</strong>. Use <strong>Siguiente</strong> / <strong>Anterior</strong>, la <strong>X</strong> o <strong>Escape</strong> para salir.', 'over', 'center'),
        pop('#dh-tour-sidebar-brand', 'Marca e inicio', 'Logo <strong>doHealth</strong> y enlace al <strong>resumen</strong> del gateway. El subtítulo indica el centro de control RFID.', 'right'),
        pop('#dh-tour-sidebar-nav', 'Menú lateral', '<strong>Inicio</strong>: métricas y atajos.<br><strong>Lectores</strong>: gestión de dispositivos, conexión y antenas.<br><strong>Tags en vivo</strong>: lecturas recientes.<br><strong>Grupos</strong>: orquestar varios lectores.<br><strong>Inventario continuo</strong>: sistemas y EPCs.<br><strong>APIs</strong>: documentación y pruebas.', 'right'),
        pop('#dh-tour-sidebar-footer', 'Pie del menú', 'Enlace rápido a <strong>/api/health</strong> para comprobar que el gateway responde.', 'right'),
        pop('#dh-tour-topbar', 'Barra superior', 'Aquí verá el <strong>título de la página</strong>. El logo lleva al inicio. En pantallas anchas aparece <strong>API estado</strong> (JSON de estado del servicio).', 'bottom'),
    ];
}

function helpStep() {
    return pop('#dh-tutorial-launch', 'Ayuda', 'Pulse el botón <strong>?</strong> en cualquier momento para repetir el tutorial de la pantalla actual.', 'left', 'end');
}

function filterSteps(steps) {
    return steps.filter((s) => {
        if (s.element == null) return true;
        const sel = typeof s.element === 'string' ? s.element : '';
        if (!sel) return true;
        return !!document.querySelector(sel);
    });
}

function homeSteps() {
    return [
        pop('#dh-tour-home-welcome', 'Cabecera', 'Resumen del producto y accesos visuales al ecosistema RFID.', 'bottom'),
        pop('#dh-tour-home-stats', 'Métricas', 'Contadores de <strong>lectores totales</strong> y <strong>conectados</strong> en tiempo real.', 'bottom'),
        pop('#dh-tour-home-shortcuts', 'Accesos rápidos', 'Iconos para ir a <strong>Lectores</strong>, <strong>Inventario</strong>, <strong>Grupos</strong> y <strong>Tags en vivo</strong>.', 'top'),
        pop('#dh-tour-home-actions', 'Acciones inferiores', '<strong>Nuevo lector</strong> abre el formulario de alta. <strong>Documentación API</strong> enlaza a la guía interactiva.', 'top'),
        pop('#dh-tour-home-fab', 'Acceso flotante', 'Atajo fijo a <strong>Nuevo lector</strong> desde el tablero.', 'left', 'end'),
    ];
}

function readersSteps() {
    return [
        pop('#dh-tour-readers-toolbar', 'Barra de acciones', '<strong>Inicio / Tags</strong>: navegación.<br><strong>Nuevo lector</strong>: alta.<br><strong>Grupos / Crear grupo</strong>: organización de lectores.<br><strong>APIs</strong>: documentación.<br>Las acciones <strong>Reset / Reboot / Status</strong> abren un <strong>modal</strong> con la respuesta JSON.', 'bottom'),
        pop('#dh-tour-readers-table', 'Tabla de lectores', 'Lista cada lector: <strong>Antenas</strong> configura puertos; <strong>Editar</strong> datos; <strong>Conectar / Desconectar</strong> sesión; <strong>Reset / Reboot / Reset antenas</strong> llaman a la API; <strong>Status</strong> abre el modal; <strong>Eliminar</strong> borra el lector.', 'top'),
        pop('#dh-tour-readers-groups', 'Grupos en esta vista', 'Resumen de grupos y miembros; enlaces para editar o eliminar.', 'top'),
        pop('#dh-tour-readers-empty', 'Sin lectores', 'Cuando no hay lectores, use <strong>Nuevo lector</strong> en la barra superior o en el menú lateral.', 'top'),
    ];
}

function readerFormSteps() {
    return [
        pop('#dh-tour-reader-form', 'Formulario de lector', 'Complete <strong>ID</strong> (único), <strong>nombre</strong>, <strong>marca</strong> (Impinj Octane es la integrada), <strong>hostname o IP</strong> y si debe quedar <strong>habilitado</strong> al arrancar. <strong>Guardar</strong> crea; <strong>Cancelar</strong> vuelve al listado.', 'right'),
    ];
}

function readerEditSteps() {
    return [
        pop('#dh-tour-reader-form', 'Editar lector', 'El <strong>ID</strong> no se puede cambiar. Ajuste nombre, host, marca, <strong>modo de operación</strong> (túnel vs continuo) y opcionalmente el <strong>sistema de inventario</strong> si usa modo continuo. Guarde o cancele.', 'right'),
    ];
}

function antennasSteps() {
    return [
        pop('#dh-tour-ant-breadcrumb', 'Navegación', 'Vuelva al listado de lectores o abra la ficha de edición del lector actual.', 'bottom'),
        pop('#dh-tour-ant-rf', 'RF y presets', 'Listas de <strong>TX/RX</strong> desde hardware si está conectado. El desplegable de <strong>entorno</strong> sugiere potencias típicas (revise normativa ERP).', 'bottom'),
        pop('#dh-tour-ant-hardware', 'Hardware Impinj', 'Modelo, firmware y número de puertos detectados en el lector.', 'top'),
        pop('#dh-tour-ant-discover', 'Detectar antenas', 'Con lector <strong>conectado</strong>, sincronice antenas físicas con el gateway.', 'top'),
        pop('#dh-tour-ant-add', 'Alta manual de antenas', 'Defina puerto, potencia, sensibilidad y si la antena está habilitada; envíe el formulario para crear o actualizar.', 'top'),
    ];
}

function groupsSteps() {
    return [
        pop('#dh-tour-groups-toolbar', 'Acciones', 'Enlaces a inicio, lectores, <strong>nuevo grupo</strong> y APIs.', 'bottom'),
        pop('#dh-tour-groups-table', 'Tabla de grupos', 'Muestra miembros y estado. <strong>Editar</strong> cambia lectores asociados; <strong>Eliminar</strong> pide confirmación.', 'top'),
        pop('#dh-tour-groups-empty', 'Sin grupos', 'Si no hay datos, use <strong>Crear grupo</strong> para empezar.', 'top'),
    ];
}

function groupFormSteps() {
    return [
        pop('#dh-tour-group-form', 'Formulario de grupo', 'Defina <strong>ID</strong> y nombre (alta), seleccione varios <strong>lectores</strong> con Ctrl/Cmd+clic, marque <strong>habilitado</strong> y envíe. En edición, el ID es de solo lectura.', 'right'),
    ];
}

function tagsSteps() {
    return [
        pop('#dh-tour-tags-header', 'Cabecera Tags', 'Título y estado del canal <strong>tiempo real</strong> (conectado o no).', 'bottom'),
        pop('#dh-tour-tags-stats', 'Métricas', 'Totales y actividad reciente (último minuto, hora, EPCs únicos).', 'bottom'),
        pop('#dh-tour-tags-controls', 'Controles', '<strong>Volver</strong> al inicio; <strong>Iniciar / detener tiempo real</strong> para ver tags en vivo; <strong>Actualizar</strong> fuerza una recarga manual.', 'bottom'),
        pop('#dh-tour-tags-filters', 'Filtros', 'Filtre por lector, EPC, antena y límite de filas; <strong>Limpiar</strong> restablece criterios.', 'top'),
        pop('#dh-tour-tags-table', 'Tabla y paginación', 'EPC, lector, antena, RSSI y hora. Use paginación si hay muchos resultados.', 'top'),
    ];
}

function invListSteps() {
    return [
        pop('#dh-tour-inv-header', 'Listado de sistemas', 'Cada sistema agrupa lectores con un <strong>ciclo global</strong> en segundos.', 'bottom'),
        pop('#dh-tour-inv-table', 'Tabla', '<strong>EPCs</strong> abre la vista en vivo de tags del sistema; <strong>Editar</strong> ajusta ciclo y miembros; <strong>Eliminar</strong> borra el sistema (con confirmación). API REST <code>GET /api/inventory-systems/{id}/live</code> para estado del ciclo y lectores.', 'top'),
    ];
}

function invFormSteps() {
    return [
        pop('#dh-tour-inv-form-nav', 'Navegación', 'Vuelva a la lista; si edita un sistema existente puede abrir <strong>EPCs en vivo</strong>.', 'bottom'),
        pop('#dh-tour-inv-form', 'Formulario de sistema', 'ID (solo en alta), nombre, <strong>ciclo global</strong> en segundos, activación y tabla de hasta <strong>6 lectores</strong> con orden y segundos por lector. Guarde o cancele.', 'right'),
    ];
}

function invEpcsSteps() {
    return [
        pop('#dh-tour-inv-epcs-nav', 'Enlaces', 'Lista de sistemas o edición del sistema cuyos EPCs está viendo.', 'bottom'),
        pop('#dh-tour-inv-epcs-panel', 'EPCs en ciclo', 'Tabla actualizada cada pocos segundos con EPCs presentes, última lectura, lector y antena. La API <code>GET /api/inventory-systems/{id}/live</code> devuelve ciclo activo, contadores y estado de lectores para dashboards.', 'top'),
    ];
}

function apiDocsSteps() {
    return [
        pop('#dh-tour-api-header', 'Cabecera y enlaces', 'Vuelva al panel, a lectores o a grupos según necesite.', 'bottom'),
        pop('#dh-tour-api-readers', 'Sección Lectores', 'Cada bloque documenta un endpoint; use <strong>Probar API</strong> para lanzar peticiones desde el navegador.', 'top'),
    ];
}

function errorSteps() {
    return [
        pop('#dh-tour-error-card', 'Mensaje de error', 'Aquí verá el código HTTP, el tipo de error y enlaces a inicio o endpoints de salud.', 'bottom'),
    ];
}

function pageSteps(route) {
    switch (route) {
        case 'home':
            return homeSteps();
        case 'readers':
            return readersSteps();
        case 'readerNew':
        case 'readerEdit':
            return route === 'readerNew' ? readerFormSteps() : readerEditSteps();
        case 'antennas':
            return antennasSteps();
        case 'groups':
            return groupsSteps();
        case 'groupNew':
        case 'groupEdit':
            return groupFormSteps();
        case 'tags':
            return tagsSteps();
        case 'invList':
            return invListSteps();
        case 'invNew':
        case 'invEdit':
            return invFormSteps();
        case 'invEpcs':
            return invEpcsSteps();
        case 'apiDocs':
            return apiDocsSteps();
        case 'errorPage':
            return errorSteps();
        default:
            return [
                pop('main', 'Contenido de la página', 'Vista actual. Use el <strong>menú lateral</strong> para navegar y la <strong>barra superior</strong> para contexto y enlaces rápidos.', 'left'),
            ];
    }
}

function buildTour() {
    const route = matchRoute(normPath());
    return filterSteps([...commonSteps(), ...pageSteps(route), helpStep()]);
}

function startTour() {
    const steps = buildTour();
    const drv = driver({
        showProgress: true,
        smoothScroll: true,
        stagePadding: 8,
        stageRadius: 8,
        overlayOpacity: 0.72,
        nextBtnText: 'Siguiente →',
        prevBtnText: '← Anterior',
        doneBtnText: 'Listo',
        progressText: '{{current}} de {{total}}',
        steps,
    });
    drv.drive();
}

function bind() {
    const btn = document.getElementById('dh-tutorial-launch');
    if (!btn) return;
    btn.addEventListener('click', () => startTour());
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bind);
} else {
    bind();
}
