-- ============================================================================
-- V17 — Vincula un Docente como Decano de una Facultad.
--
-- Antes no existía ninguna relación Decano↔Facultad en el esquema (ya se
-- había documentado esta limitación en la corrección de seguridad anterior:
-- AuthorizationService no podía acotar a un DECANO a "su" facultad porque no
-- había dónde consultarla). id_docente_decano es nullable: una facultad
-- puede no tener decano asignado todavía.
-- ============================================================================

ALTER TABLE facultad ADD COLUMN id_docente_decano INTEGER REFERENCES docente(id_docente);
