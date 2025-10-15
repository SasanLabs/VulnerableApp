description = "BLIND_SQL_INJECTION_URL_PARAM_APPENDED_DIRECTLY_TO_QUERY",
            payload = "BLIND_SQL_INJECTION_PAYLOAD_LEVEL_1")
    @VulnerableAppRequestMapping(
            value = LevelConstants.LEVEL_1,
            htmlTemplate = "LEVEL_1/SQLInjection_Level1")
    public ResponseEntity<String> getCarInformationLevel1(
            @RequestParam Map<String, String> queryParams) {
        String id = queryParams.get(Constants.ID);
        BodyBuilder bodyBuilder = ResponseEntity.status(HttpStatus.OK);
        return applicationJdbcTemplate.query(
                "select * from cars where id=" + id,
                (rs) -> {
                    if (rs.next()) {
                        return bodyBuilder.body(CAR_IS_PRESENT_RESPONSE);
                    }
                    return bodyBuilder.body(
                            ErrorBasedSQLInjectionVulnerability.CAR_IS_NOT_PRESENT_RESPONSE);
                });
    }
    @AttackVector(
