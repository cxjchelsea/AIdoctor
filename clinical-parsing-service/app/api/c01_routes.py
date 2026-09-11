"""Governed C01 capability routes for the new clinical runtime path."""
from fastapi import APIRouter

from app.models.c01_u01 import C01U01Request, C01U01Result
from app.services.c01_u01_service import C01U01ClinicalUnderstandingService


router = APIRouter()
service = C01U01ClinicalUnderstandingService()


@router.post("/capabilities/c01/u01/interpret", response_model=C01U01Result)
async def interpret_u01(request: C01U01Request) -> C01U01Result:
    """Return C01 candidates for U01. No Clinical State mutation is performed."""
    return await service.interpret(request)
