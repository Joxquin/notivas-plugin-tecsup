# NotiVas Plugin: Tecsup

Plugin de integración institucional para NotiVas-KMP que permite consultar y sincronizar notas, asistencias y horarios de Tecsup mediante la API oficial de u-planner v3.

## Características

- Autenticación OAuth vía Azure AD B2C / Google SSO (`tecsup-api.u-planner.com`).
- Sincronización de notas por periodo académico y sección.
- Registro detallado de inasistencias y límite permitido de faltas.
- Consulta de horarios de clases y eventos por día.
- Integración nativa con el Copiloto IA de NotiVas mediante Tool Calling (`get_tecsup_grades`, `get_tecsup_schedule`, `get_tecsup_attendance`).
- Diseño visual dual adaptativo: HyperOS MIUIX y Material Design 3 con soporte Monet.

## Estructura del Proyecto

- `me.joxquin.notivas.plugins.tecsup.api`: Cliente HTTP Ktor y definición de endpoints u-planner v3.
- `me.joxquin.notivas.plugins.tecsup.model`: Modelos de datos serializables (`kotlinx.serialization`).
- `me.joxquin.notivas.plugins.tecsup.session`: Gestión segura del token de sesión (`Bearer Token`).
- `me.joxquin.notivas.plugins.tecsup.copilot`: Implementación de `CopilotToolHandler`.
- `me.joxquin.notivas.plugins.tecsup.ui`: Componentes visuales para la pestaña de Utilidades y pantallas modulares.
