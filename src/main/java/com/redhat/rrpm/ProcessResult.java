/*
    Copyright 2013 Red Hat, Inc. and/or its affiliates.

    This file is part of rrpm.

    rrpm is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    rrpm is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with rrpm.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.redhat.rrpm;

/**
 * @author Burak Serdar (bserdar@redhat.com)
 */
public class ProcessResult {

    private int returnCode;
    private String stdOut;
    private String stdErr;


    /**
     * Gets the value of returnCode
     *
     * @return the value of returnCode
     */
    public int getReturnCode() {
        return this.returnCode;
    }

    /**
     * Sets the value of returnCode
     *
     * @param argReturnCode Value to assign to this.returnCode
     */
    public void setReturnCode(int argReturnCode) {
        this.returnCode = argReturnCode;
    }

    /**
     * Gets the value of stdOut
     *
     * @return the value of stdOut
     */
    public String getStdOut() {
        return this.stdOut;
    }

    /**
     * Sets the value of stdOut
     *
     * @param argStdOut Value to assign to this.stdOut
     */
    public void setStdOut(String argStdOut) {
        this.stdOut = argStdOut;
    }

    /**
     * Gets the value of stdErr
     *
     * @return the value of stdErr
     */
    public String getStdErr() {
        return this.stdErr;
    }

    /**
     * Sets the value of stdErr
     *
     * @param argStdErr Value to assign to this.stdErr
     */
    public void setStdErr(String argStdErr) {
        this.stdErr = argStdErr;
    }

    public String toString() {
        return "returnCode:"+returnCode+"\n"+
            "stdOut:"+stdOut+"\n"+
            "stdErr:"+stdErr;
    }
}
