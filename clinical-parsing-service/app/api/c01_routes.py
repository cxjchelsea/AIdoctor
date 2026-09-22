"""Governed C01 capability routes for the new clinical runtime path."""
from fastapi import APIRouter

from app.models.c01_u01 import C01U01Request, C01U01Result
from app.models.c01_u02 import C01U02Request, C01U02Result
from app.services.c01_u01_service import C01U01ClinicalUnderstandingService
from app.services.c01_u02_service import C01U02ClinicalUnderstandingService


router = APIRouter()
u01_service = C01U01ClinicalUnderstandingService()
u02_service = C01U02ClinicalUnderstandingService()


@router.post("/capabilities/c01/u01/interpret", response_model=C01U01Result)
async def interpret_u01(request: C01U01Request) -> C01U01Result:
    """Return C01 candidates for U01. No Clinical State mutation is performed."""
    return await u01_service.interpret(request)


@router.post("/capabilities/c01/u02/interpret", response_model=C01U02Result)
async def interpret_u02(request: C01U02Request) -> C01U02Result:
    """Return Observation Candidates for U02. No Clinical State mutation is performed."""
    return await u02_service.interpret(request)
