
ALTER TABLE t_employee_vacation_remaining rename TO t_employee_remaining_leave;

ALTER TABLE t_employee_remaining_leave rename ALTER column carry_vacation_days_from_previous_year TO remaining_from_previous_year;

